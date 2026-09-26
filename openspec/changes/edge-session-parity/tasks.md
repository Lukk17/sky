# Tasks

## 1. Investigate and record edge baselines

- [x] 1.1 Document current oauth2-proxy effective flags (rendered deployment from `values.yaml` plus overlays: `/oauth2/start`, `/oauth2/sign_out`, `/oauth2/userinfo`, `/oauth2/auth` availability, `setAuthorizationHeader`/`passAuthorizationHeader` values, `cookie-csrf-per-request`/`cookie-csrf-expire`) and the gateway contract source lines (`SessionController`, `SecurityConfig` `!local` chain, `OriginCheckWebFilter`, `sky-auth.service`) and verify the record names exact file paths and flag values.
- [x] 1.2 Audit every `/api/**` ingress path across service charts for auth-annotation coverage (which carry `auth-url`/`auth-signin`, which are intentionally public) and verify the audit lists each path with its verdict.

## 2. Map the contract onto the cluster edge

- [x] 2.1 Add the `GET /api/session` session ingress plus minimal responder (email from auth-request headers, session-bound CSRF token, empty-body 401 anonymous) and verify `helm template` renders the new ingress with `auth-url`/`auth-signin` and no service chart touched.
- [x] 2.2 Add the `/oauth2/authorization/keycloak` to `/oauth2/start` login alias with return-to-frontend-origin and verify an anonymous curl to the alias reaches the Keycloak login on k3d.
- [x] 2.3 Add the `POST /logout` to `/oauth2/sign_out` mapping with post-logout redirect to the frontend origin plus Keycloak end-session and verify the rewrite preserves POST on k3d.
- [x] 2.4 Scope auth annotations so anonymous `GET /api/v1/offers/**` and `POST /api/v1/search` stay public while all other `/api/**` paths require auth, and verify rendered templates show the split with no other path losing its gate.
- [x] 2.5 Record the CSRF decision (mechanism-per-edge: compose session token plus Origin check; k8s Strict session cookies plus oauth2-proxy cookie-CSRF, services untouched) in the chart values/comments and verify `SameSite=Strict` on session cookies plus a green login-to-logout lifecycle on k3d.
- [x] 2.6 Confirm Bearer passthrough verbatim (`setAuthorizationHeader`/`passAuthorizationHeader` already true) and verify an upstream-echo curl shows the identical `Authorization` value through the cluster edge.

## 3. Frontend per-environment story

- [x] 3.1 Confirm zero frontend differences (identical paths, `X-XSRF-TOKEN` name, no edge branching; origin-only config) and verify by diffing the frontend auth paths against the cluster paths with no mismatch, recording any forced deviation as a cluster-side fix.

## 4. Verification through the k3d edge

- [x] 4.1 Run the Bruno k8s environment green against the k3d edge and verify all session, public-route, and Bearer cases pass.
- [x] 4.2 Run a browser or curl login-to-logout lifecycle through the k3d edge (login alias, `GET /api/session` 200 with email plus csrfToken, CSRF-protected mutation, `POST /logout`, `GET /api/session` 401) and verify each step's status code and body shape.
