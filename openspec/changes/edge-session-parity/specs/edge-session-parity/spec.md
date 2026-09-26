# Spec Delta

## Purpose

The Kubernetes edge (nginx-ingress plus oauth2-proxy) serves the identical session-auth contract the Angular frontend already uses against sky-gateway, so one frontend build works against either edge with services untouched.

## ADDED Requirements

### Requirement: K8s edge serves the session endpoint

The Kubernetes edge SHALL expose `GET /api/session` returning `{email}` plus `csrfToken` for an authenticated browser session and `401` with an empty body otherwise, matching the gateway `SessionController` shape (`email` from the OIDC email claim, `csrfToken` bound to the session).

#### Scenario: Authenticated session returns identity and CSRF token

- **WHEN** a browser with a valid oauth2-proxy session calls `GET /api/session` through the cluster ingress
- **THEN** it receives 200 with `{email, csrfToken}` where `csrfToken` is accepted on subsequent mutating calls

#### Scenario: Anonymous session probe answers 401 without redirect

- **WHEN** a sessionless caller requests `GET /api/session` through the cluster ingress
- **THEN** it receives 401 with an empty body and never a login redirect

### Requirement: K8s edge login and logout paths match the frontend contract

Browser navigation to `/oauth2/authorization/keycloak` SHALL start the Keycloak login flow through the cluster edge and return to the frontend origin after success; `POST /logout` SHALL end the session (oauth2-proxy session plus Keycloak SSO) and a subsequent `GET /api/session` SHALL answer 401.

#### Scenario: Login starts from the gateway-identical path and returns to the frontend

- **WHEN** an anonymous browser navigates to `/oauth2/authorization/keycloak` on the cluster host
- **THEN** it reaches the Keycloak login and, after success, lands back on the frontend origin with an established session

#### Scenario: Logout ends the session on both layers

- **WHEN** an authenticated browser posts to `/logout` on the cluster host
- **THEN** the oauth2-proxy session and the Keycloak SSO session end, and `GET /api/session` afterwards answers 401

### Requirement: Public routes and Bearer passthrough are identical on both edges

Anonymous `GET /api/v1/offers/**` and `POST /api/v1/search` SHALL stay public through the cluster ingress (no `auth-url` on those paths); every other `/api/**` path SHALL require authentication; caller-supplied `Authorization: Bearer` headers SHALL pass through oauth2-proxy (`setAuthorizationHeader` / `passAuthorizationHeader`) to the services, which continue validating JWTs themselves.

#### Scenario: Anonymous offer reads stay public in the cluster

- **WHEN** an anonymous caller requests `GET /api/v1/offers` or `POST /api/v1/search` on the cluster host
- **THEN** the request reaches sky-offer without a login redirect

#### Scenario: Anonymous secured API call answers 401 without redirect

- **WHEN** a sessionless, tokenless caller requests any other `/api/**` path on the cluster host
- **THEN** it receives 401 with an empty body and never a login redirect

#### Scenario: Bearer call reaches the service with its token intact

- **WHEN** a caller sends `Authorization: Bearer <jwt>` to a secured API path on the cluster host
- **THEN** the upstream service receives the identical header value and validates it as today

### Requirement: CSRF stance per edge is decided and enforced, frontend contract identical

CSRF protection SHALL be mechanism-per-edge with an identical frontend contract (`X-XSRF-TOKEN` on mutating calls, session token from `GET /api/session`): the compose edge (sky-gateway) uses a session token plus `OriginCheckWebFilter` Origin check; the Kubernetes edge uses `SameSite=Strict` session cookies (`cookie-samesite=strict` in `oauth2-proxy/values.yaml`, verified `SameSite=Strict` on `_oauth2_proxy` and `_oauth2_proxy_csrf_*` Set-Cookie responses) plus oauth2-proxy per-request cookie-CSRF (`cookie-csrf-per-request=true`, 15m expiry in the deployment template). The cluster edge SHALL NOT rely on services (they ignore `X-XSRF-TOKEN` and stay untouched). The nginx Origin/Referer snippet stays recorded-but-unapplied: the ingress-nginx admission webhook rejects configuration snippets, so until controller policy changes the k8s token layer is oauth2-proxy cookie-CSRF with Strict cookies as the cross-site backstop.

#### Scenario: Cross-site mutation with session cookie never reaches a service

- **WHEN** a cross-origin mutating request targets the cluster host
- **THEN** the browser withholds the `SameSite=Strict` session cookie so no authenticated service call happens, and any forged request that does arrive cookie-authenticated without a matching token is rejected by oauth2-proxy cookie-CSRF before any service call happens

#### Scenario: SameSite=Strict does not break the login flow

- **WHEN** a browser completes the Keycloak callback (`/oauth2/callback` on the same site as the session cookie) and then calls secured screens
- **THEN** the callback establishes the session and `GET /api/session` afterwards answers 200, verified by a login-to-logout lifecycle on k3d

### Requirement: Single session owner in Kubernetes

Exactly one component SHALL own the browser session on the cluster edge: `oauth2-proxy`. No `sky-gateway` session layer SHALL be introduced in Kubernetes (it ships no Helm chart), so there is never a double login or competing logout semantics between two session cookies.

#### Scenario: No double login across anonymous and secured cluster routes

- **WHEN** a signed-in user moves between anonymous and secured routes on the cluster host
- **THEN** at most one login interaction occurs and the session established by it satisfies both

### Requirement: Zero frontend differences across edges

The Angular app SHALL use identical paths (`/api/session`, `/oauth2/authorization/keycloak`, `/logout`, `/api/v1/**`), the identical CSRF header name (`X-XSRF-TOKEN`), and no edge-specific branching; only the origin (base URL) SHALL differ per environment.

#### Scenario: Same frontend build works against either edge

- **WHEN** the frontend built once is pointed at the compose edge or the cluster edge
- **THEN** login, session probe, CSRF-protected mutations, public reads, and logout all behave identically
