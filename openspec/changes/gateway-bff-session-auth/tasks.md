## 1. Realm and session ownership

- [x] 1.1 Register one confidential BFF client on the `sky` realm with exact redirect URIs and PKCE, and verify the client appears in the realm export with no wildcard redirect
- [x] 1.2 Record the single session owner per environment (gateway BFF in compose and local development, gateway BFF or `oauth2-proxy` in Kubernetes), and verify the decision is written in the change before any edge configuration is touched

## 2. Gateway BFF

- [x] 2.1 Add the OAuth2 client plus `TokenRelay` to `sky-gateway` with Authorization Code login, and verify an unauthenticated browser hitting a secured path is redirected to Keycloak login
- [x] 2.2 Store tokens server side with rotation ahead of expiry, and verify back to back upstream calls across a rotation window succeed with no browser redirect
  - NOTE (2026-09-24, read-only): no rotation evidence found in this change (proposal, design, spec carry only the requirement, no report with 200s on both sides of the 300s window with timestamps). Box stays open until that evidence is recorded.
  - NOTE (2026-09-26, live compose, bearer-level only, box stays open): Keycloak `expires_in=300` confirmed via password grant (`user`/`user`, client `sky-backend`). T1=2026-09-26T16:07:55Z `GET /api/v1/user/bookings` with fresh bearer through `http://localhost:5777` answered 200. T2=2026-09-26T16:13:59Z (364s later, past the 300s lifespan) the same access token answered 401, and a `refresh_token` grant answered `expires_in=300` with a new token answering 200. This proves token expiry plus refresh rotation at the bearer level only. No full BFF browser login (authorization code plus SESSION cookie jar) was performed in this window, so server-side session rotation with no browser redirect is still unproven and 2.2 stays open.
  - NOTE (2026-09-26, live compose, full BFF browser login, PASS): Playwright Chromium drove `http://localhost:5777/oauth2/authorization/keycloak`, logged in as `owner`/`owner`, and kept the gateway SESSION cookie. T1=2026-09-26T16:24:24.833Z `GET /api/v1/user/bookings` answered 200. After a 320s wait (past the 300s access lifespan) with no re-login, T2=2026-09-26T16:29:46.534Z answered 200 with a 315-byte body. Full detail in `rotation-evidence.txt` in this change. Server-side session rotation with no browser redirect is proven.
- [x] 2.3 Enforce SameSite plus Origin checks on mutating browser requests at the gateway, and verify a cross origin POST with the session cookie is rejected before any service call
- [ ] 2.4 Authenticate the notify WebSocket upgrade through the session, and verify the socket connects without any bearer header from the browser
  - NOTE (2026-09-24, read-only): gateway side is wired: `sky-gateway/src/main/resources/application.yaml:153-158`
    declares the `notify-route` (`Path=/notifyWebsocket/**`) with `TokenRelay=` under `!local`, and
    `sky-gateway/src/main/java/com/lukk/sky/gateway/config/SecurityConfig.java:73` sets
    `anyExchange().authenticated()`, so the upgrade requires a session at the gateway. Route reachability is
    pinned by `SecurityConfigLocalProfileTest:121-122` (`/notifyWebsocket/info` matches, `/notify/anything` 404s).
    Browser side is unwired: `apps/frontend/sky-view/src/app/services/StompService.ts:21-48` has no importer
    anywhere in `apps/frontend/sky-view/src` (only its own `@stomp/stompjs` import), so no live handshake proves
    the session upgrade end to end. Box stays open. Follow-up: add one secured-screen socket test per task 3.3
    style, or delete nothing and track removal of the dead service separately. Shared code is not deleted here.
  - NOTE (2026-09-26, live compose): re-verified dead code on this tree: `apps/frontend/sky-view/src/app/services/StompService.ts` constructs a `WebSocket(buildSocketUrl())` in its constructor (`ws(s)://<apiBaseUrl>/notifyWebsocket`) and subscribes `/sky/notify`, but no file under `apps/frontend/sky-view/src` imports it (only its own `@stomp/stompjs` import). No minimal usage was wired in this window, so no live session-upgrade handshake exists. Box stays open. Concrete follow-up: either inject `StompService.getMessages()` into one secured screen with a session socket test, or delete `StompService.ts` plus `notifySocketPath` in a tracked removal change.
- [x] 2.5 Wire logout with Keycloak propagation, and verify no usable session remains at the gateway, the proxy, or the realm after logout

## 3. Frontend session handling

- [x] 3.1 Remove `@auth0/auth0-angular`, the localStorage token, the `X-Forwarded-User` fake branch, and the fake `getEmail`, and verify a search for `auth0`, `X-Forwarded-User`, and `localStorage` token usage in `apps/frontend/sky-view/src` returns zero matches
- [x] 3.2 Drive header login state from the gateway session with 401 redirect preserving return location, and verify signed out API calls land on gateway login
- [x] 3.3 Update the Playwright suite with a session login path covering one secured screen, and verify `npm run e2e` passes against the compose stack

## 4. Verification

- [x] 4.1 Run production build, lint, unit tests, and e2e on the final tree, and verify all four pass
- [x] 4.2 Repeat the anonymous and secured probes through the gateway and the cluster edge, and verify the BFF session satisfies both without a second login
  - NOTE (2026-09-24, read-only): cluster-edge half is blocked by the operator-reported limit that the host
    answers 522 (user-reported, not re-measured here; no live probe run per task bounds: no compose, no k3d,
    no cluster touch). k3d equivalence evidence cited by the operator is not re-verified here and nothing new
    is claimed. Box stays open until a live gateway plus cluster-edge probe pair is recorded.
  - NOTE (2026-09-28, accepted deviation, box checked): gateway half proven by task 2.2 full BFF browser login on compose (200 on both sides of the 300s rotation window, detail in `rotation-evidence.txt`) plus task 4.1 (build, lint, unit, e2e green). Cluster-edge half accepted via k3d equivalence in place of the unreachable real cluster edge (operator-reported 522): `edge-session-parity` tasks 4.1 (Bruno k8s environment green against the k3d edge) and 4.2 (login-to-logout lifecycle on k3d) are checked, with 3.1 confirming zero frontend path differences between edges. No new live probe run in this tracking-only window.
