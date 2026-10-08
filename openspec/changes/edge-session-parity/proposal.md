# Proposal

## Why

The Angular frontend was built against the compose edge (`sky-gateway` on :5777), but production traffic enters through a different edge (`nginx-ingress` plus `oauth2-proxy`), and the two edges do not serve the same session contract today. The cluster ingress has no session endpoint, no mapped login/logout paths, and no documented parity for public routes, Bearer passthrough, and CSRF, so a frontend verified locally can break in production with no code change of its own.

## What Changes

- Map the exact frontend contract onto the Kubernetes edge with nginx-ingress path and annotation changes plus oauth2-proxy configuration only: `GET /api/session` (email plus csrfToken or 401), browser navigation to `/oauth2/authorization/keycloak` starting login with return to the frontend origin, `POST /logout` ending the session, anonymous `GET /api/v1/offers` and `POST /api/v1/search` staying public, Bearer calls passing through with service-side JWT validation.
- Decide and record the oauth2-proxy CSRF stance for cookie-authenticated browser calls (services ignore unknown headers; the gateway validates `X-XSRF-TOKEN` against the session copy plus an Origin check).
- Confirm the TokenRelay equivalent: `setAuthorizationHeader` / `passAuthorizationHeader` (already `true` in `values.yaml`) forward `Authorization` upstream, including caller-supplied Bearer tokens.
- Decide and record the frontend build/config story per environment, preferring zero frontend differences (same paths, same header name, environment-supplied origin only).
- Keep every service interior untouched; all changes live in Helm values, ingress annotations, and oauth2-proxy flags.
- Add verification: Bruno k8s environment green plus a browser or curl login-to-logout lifecycle through the k3d edge.

## Capabilities

### New Capabilities

- `edge-session-parity`: the Kubernetes edge serves the identical session-auth contract the frontend already uses against `sky-gateway` (session endpoint shape, login/logout paths and redirects, public-route set, Bearer passthrough, CSRF stance, single session owner), with services untouched.

### Modified Capabilities

- None. The `gateway-bff-auth` capability (prior change `gateway-bff-session-auth`) already defines the contract; this change maps it onto the second edge without changing its requirements.

## Impact

- Affected: `config/k8s/helm/api-gateway/oauth2-proxy/` (values, deployment flags, ingress), service-chart ingress annotations for the session endpoint route and auth scoping (`auth-url`, `auth-signin`, public-route exclusions), Bruno k8s environment, e2e runbook for the k3d lifecycle. Frontend (`apps/frontend/sky-view`) expected to need no change.
- Not affected: service interiors (controllers, security chains, JWT validation), `sky-gateway` code, database schemas, Kafka topics.
- Risk: oauth2-proxy `/oauth2/*` path prefix already claims the namespace the gateway login path lives under; mapping `/oauth2/authorization/keycloak` identically may require an ingress-level alias or redirect rather than a literal upstream path. CSRF semantics cannot be copied verbatim (oauth2-proxy has its own cookie-CSRF flags, already `cookie-csrf-per-request=true` in the deployment template) and must be decided, not assumed.
