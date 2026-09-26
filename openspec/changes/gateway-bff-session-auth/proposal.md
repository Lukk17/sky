## Why

The browser and the services authenticate against two different issuers: the Angular app logs in through Auth0 while every service validates Keycloak JWTs from the `sky` realm, so no token the frontend can obtain passes backend validation and the `SkyAuthService` token plumbing is an unwired stub. A Backend for Frontend session at the gateway removes tokens from the browser entirely, which is the strongest posture against token theft short of sender constrained tokens.

## What Changes

- `sky-gateway` becomes an OAuth2 BFF: Authorization Code plus PKCE login against the `sky` realm, server side session in an HttpOnly Secure SameSite cookie, token storage and refresh rotation inside the gateway, `TokenRelay` attaching the current access token upstream.
- The Angular app drops `@auth0/auth0-angular` and all token handling: no stored tokens, no `Authorization` header construction, session cookie sent automatically, 401 redirects to gateway login.
- `SkyAuthService` loses the localStorage token, the `X-Forwarded-User` fake header branch, and the fake `getEmail`; the header component drives login state from the gateway session.
- Service interior is unchanged: each service keeps validating the relayed JWT (signature, issuer, audience `sky-backend`, realm roles).
- CSRF defenses return on mutating browser calls (SameSite plus Origin checks at minimum) now that a cookie authenticates the browser.
- **BREAKING**: the Auth0 tenant configuration is removed; any flow depending on Auth0 issued tokens stops working by design.

## Capabilities

### New Capabilities

- `gateway-bff-auth`: gateway session login, server side token storage and rotation, token relay to services, frontend session handling, CSRF rules for cookie authenticated browser calls.

### Modified Capabilities

None. Service side validation requirements are unchanged by design.

## Impact

- `sky-gateway` (new `spring-boot-starter-oauth2-client` dependency, session store, login and logout routes, logout propagation to Keycloak).
- `apps/frontend/sky-view` (Auth0 SDK removal, `SkyAuthService` rewrite, header and auth components, e2e login path).
- Keycloak `sky` realm (one confidential BFF client with exact redirect URIs; public SPA client retired).
- Compose stack (gateway session configuration, shared issuer already present).
- k8s edge (defines how `oauth2-proxy` and the gateway BFF divide the browser session so exactly one of them owns it per environment).
