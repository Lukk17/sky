# Security self-review: gateway BFF session auth (read-only, no fixes applied)

Scope: `sky-gateway/.../config/SecurityConfig.java`, `OriginCheckWebFilter.java`, `SessionController.java`, CSRF token store plus interceptor (`apps/frontend/sky-view/src/app/services/csrf-token.store.ts`, `session-cookie.interceptor.ts`), `sky-auth.service.ts`. Reviewed 2026-09-24 without running services.

## Findings

### [MEDIUM] Session cookie flags are framework defaults, not explicit
`SecurityConfig.java:37-110` and `application.yaml:1-206` set no `server.reactive.session.cookie` attributes (no explicit `http-only`, `secure`, `same-site`). Session therefore relies on Spring Session/Spring Boot defaults. If the deployment terminates TLS at ingress without forwarded-proto handling, `Secure` may not hold on the wire. Evidence: no `session.cookie` key anywhere under `sky-gateway/`.
Anonymous parity note: anonymous reads (`SecurityConfig.java:72-73`) still set/refresh the session cookie path identically; no separate anonymous cookie handling found.

### [MEDIUM] OAuth2 success handler redirects to a configured frontend URL with no allow-list check at the call site
`SecurityConfig.java:86-87` passes `frontendUrl` (`sky-gateway.frontend-url`, default `http://localhost:4200`) straight into `RedirectServerAuthenticationSuccessHandler`. A misconfigured value (or an env-provided absolute URL) becomes an open-redirect primitive post-login. No validation of scheme, host, or path at the call site. Evidence: `SecurityConfig.java:37,86-87`.

### [MEDIUM] Post-login return path stored in sessionStorage and passed to router.navigate without validation
`sky-auth.service.ts:45-48,58-62` stores any caller-supplied `returnPath` under `postLoginPath`; `auth.component.ts:21` and `app.component.ts:33-39` feed the consumed value directly into `router.navigate([...])`. A crafted stored value (e.g. via reflected link state or devtools) can route the freshly authenticated session to an unintended in-app screen. Angular router navigation stays in-app (not an off-site redirect), which bounds this to in-app confusion rather than credential exfiltration. Evidence: `sky-auth.service.ts:45-62`, `auth.component.ts:18-24`, `app.component.ts:33-40`.

### [LOW] Origin check compares hosts only and lets headerless mutating requests through
`OriginCheckWebFilter.java:31-53`: safe methods skip the check; mutating requests with neither `Origin` nor `Referer` pass through by design (documented for non-browser callers); comparison is `host.equalsIgnoreCase` with no scheme or port check, so `http` vs `https` (or an adjacent port) on the same host passes. CSRF token validation (`SecurityConfig.java:91-102`) remains the backstop for cookie-carrying browser calls. Evidence: `OriginCheckWebFilter.java:27-53`.

### [LOW] CSRF bypass when no Cookie header is present
`SecurityConfig.java:93-102`: requests without `Authorization` and without `Cookie` skip CSRF (`notMatch`). Correct for anonymous reads, but any future anonymous mutating endpoint would inherit the exemption silently. Current anonymous surface is read-only (`GET /api/v1/offers/**`, `POST /api/v1/search` per `SecurityConfig.java:72-73`), so impact stays low. Note `POST /api/v1/search` is anonymous yet CSRF-enforced once a cookie exists, which is the intended behaviour. Evidence: `SecurityConfig.java:72-73,91-102`.

### [LOW] CSRF token lives in memory only and goes stale silently
`csrf-token.store.ts:4-14` holds the token in a plain in-memory field; `sky-auth.service.ts:32-43` clears it only when `/api/session` errors. A failed refresh or a rotated server-side token leaves the interceptor (`session-cookie.interceptor.ts:16-19`) sending a stale `X-XSRF-TOKEN` until the next 403/401 surfaces it. No proactive refresh or 403-triggered re-fetch. Evidence: `csrf-token.store.ts:4-14`, `sky-auth.service.ts:32-43`, `session-cookie.interceptor.ts:16-19`.

### [LOW] Logout is client-driven and tolerates silent failure
`sky-auth.service.ts:50-67` POSTs to `/logout` and navigates to `/home` on both success and error (`afterLogout` in both branches). A failed server-side logout (network drop, 5xx) still clears local state and shows the logged-out UI while the gateway session and Keycloak SSO session may survive. Gateway side uses `OidcClientInitiatedServerLogoutSuccessHandler` (`SecurityConfig.java:89-90`), which propagates only on success. Evidence: `sky-auth.service.ts:50-67`, `SecurityConfig.java:89-90`.

### [INFO] CORS with credentials is narrowly scoped
`SecurityConfig.java:40-49`: `allowedOrigins` is exactly `[frontendUrl]`, `allowCredentials=true`, methods limited to GET/POST/PUT/PATCH/DELETE/OPTIONS, but `allowedHeaders=*`. Narrow origin plus credentials is correct; wildcard headers widen preflight surface slightly without exposing tokens (tokens never leave the gateway per `SessionController.java:24-37` and TokenRelay design). Evidence: `SecurityConfig.java:40-49`.

### [INFO] No token leakage found in reviewed surface
`/api/session` returns only `{email, csrfToken}` (`SessionController.java:24-37`); the interceptor attaches only `X-XSRF-TOKEN` and `withCredentials` (`session-cookie.interceptor.ts:15-19`); login/logout use top-level navigation and gateway endpoints, never token query params (`sky-auth.service.ts:17-19,45-48`). CSRF token repository uses the session with header name `X-XSRF-TOKEN` (`SecurityConfig.java:106-110`), matching the interceptor. Local profile disables CSRF/CORS checks entirely (`SecurityConfig.java:51-61`); accepted as local-only but worth keeping out of any shared environment.

## Above-low summary
Three MEDIUM findings (session cookie flags implicit; OAuth2 success redirect target unvalidated; in-app return path unvalidated), no HIGH or CRITICAL. No fixes applied per task bounds.
