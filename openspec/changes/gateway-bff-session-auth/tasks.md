## 1. Realm and session ownership

- [x] 1.1 Register one confidential BFF client on the `sky` realm with exact redirect URIs and PKCE, and verify the client appears in the realm export with no wildcard redirect
- [x] 1.2 Record the single session owner per environment (gateway BFF in compose and local development, gateway BFF or `oauth2-proxy` in Kubernetes), and verify the decision is written in the change before any edge configuration is touched

## 2. Gateway BFF

- [x] 2.1 Add the OAuth2 client plus `TokenRelay` to `sky-gateway` with Authorization Code login, and verify an unauthenticated browser hitting a secured path is redirected to Keycloak login
- [ ] 2.2 Store tokens server side with rotation ahead of expiry, and verify back to back upstream calls across a rotation window succeed with no browser redirect
  - NOTE (2026-09-24, read-only): no rotation evidence found in this change (proposal, design, spec carry only the requirement, no report with 200s on both sides of the 300s window with timestamps). Box stays open until that evidence is recorded.
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
- [x] 2.5 Wire logout with Keycloak propagation, and verify no usable session remains at the gateway, the proxy, or the realm after logout

## 3. Frontend session handling

- [x] 3.1 Remove `@auth0/auth0-angular`, the localStorage token, the `X-Forwarded-User` fake branch, and the fake `getEmail`, and verify a search for `auth0`, `X-Forwarded-User`, and `localStorage` token usage in `apps/frontend/sky-view/src` returns zero matches
- [x] 3.2 Drive header login state from the gateway session with 401 redirect preserving return location, and verify signed out API calls land on gateway login
- [x] 3.3 Update the Playwright suite with a session login path covering one secured screen, and verify `npm run e2e` passes against the compose stack

## 4. Verification

- [x] 4.1 Run production build, lint, unit tests, and e2e on the final tree, and verify all four pass
- [ ] 4.2 Repeat the anonymous and secured probes through the gateway and the cluster edge, and verify the BFF session satisfies both without a second login
  - NOTE (2026-09-24, read-only): cluster-edge half is blocked by the operator-reported limit that the host
    answers 522 (user-reported, not re-measured here; no live probe run per task bounds: no compose, no k3d,
    no cluster touch). k3d equivalence evidence cited by the operator is not re-verified here and nothing new
    is claimed. Box stays open until a live gateway plus cluster-edge probe pair is recorded.
