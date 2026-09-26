## Purpose

The gateway owns the browser session and relays validated caller tokens so the Angular app never handles credentials while every service keeps verifying the caller independently.

## ADDED Requirements

### Requirement: Gateway session login

The gateway SHALL authenticate the browser through the `sky` realm Authorization Code flow with PKCE and hold the resulting session in an HttpOnly Secure SameSite cookie. Tokens SHALL live server side only and never reach the browser in any response, page, or log.

#### Scenario: Unauthenticated browser navigation is redirected to login

- **WHEN** a browser without a gateway session navigates to a secured frontend route or non API path
- **THEN** the gateway redirects it to the Keycloak login and, after success, returns it to the frontend origin

#### Scenario: Unauthenticated API call answers 401 without redirect

- **WHEN** a sessionless caller requests an `/api/**` path
- **THEN** the gateway answers 401 with an empty body and never issues a login redirect, so script driven callers never fetch login pages into their cookie jar

#### Scenario: Tokens never reach the browser

- **WHEN** login, refresh, and API responses pass through the gateway
- **THEN** no response carries an access, ID, or refresh token in body, header, or cookie readable by script

### Requirement: Token relay to services

The gateway SHALL attach the current access token as `Authorization: Bearer` on every upstream service call and refresh it server side before expiry. Each service SHALL continue validating signature, issuer, audience `sky-backend`, and realm roles exactly as today.

#### Scenario: Relayed call is authorized downstream

- **WHEN** a session authenticated browser calls a secured API path
- **THEN** the upstream service receives a valid bearer token and answers according to the caller's roles

#### Scenario: Expired access token is refreshed without browser involvement

- **WHEN** the held access token expires while the session and refresh token remain valid
- **THEN** the next upstream call succeeds after a server side refresh with no browser redirect

#### Scenario: Ended session stops relaying

- **WHEN** the user logs out or the session expires and a secured path is requested
- **THEN** the gateway redirects to login and no upstream call is made

### Requirement: Frontend without token handling

The Angular app SHALL NOT store tokens, construct `Authorization` headers, or depend on Auth0. Login state SHALL derive from the gateway session, and a 401 SHALL send the browser to the gateway login.

#### Scenario: Secured screen loads through the session

- **WHEN** a signed in user opens bookings or messages
- **THEN** the app calls the API with no explicit auth header and renders the data

#### Scenario: Signed out user is redirected

- **WHEN** an API call answers 401
- **THEN** the app navigates the browser to the gateway login preserving the return location

### Requirement: CSRF protection on cookie authenticated calls

Mutating browser requests SHALL carry a session bound CSRF token plus Origin checking at the gateway. The token SHALL be delivered inside the authenticated session payload, held in frontend memory rather than in a cookie, sent back as `X-XSRF-TOKEN`, and validated against the server side session copy. Requests failing either check SHALL be rejected before reaching any service.

#### Scenario: Cross site mutation is rejected

- **WHEN** a cross origin POST arrives with the session cookie but without a matching Origin
- **THEN** the gateway rejects it and no service call happens

### Requirement: Single session owner per environment

Exactly one component SHALL own the browser session in each environment: the gateway BFF in compose and local development, and either the gateway BFF or `oauth2-proxy`, but never both, in Kubernetes.

#### Scenario: No double login in Kubernetes

- **WHEN** a signed in user moves between anonymous and secured routes on the cluster
- **THEN** at most one login interaction occurs and the session established by it satisfies both layers
