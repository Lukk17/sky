## Context

Spring WebSocket / STOMP doesn't automatically apply the HTTP Spring Security filter chain to inbound STOMP frames. The HTTP handshake (the `GET /notifyWebsocket` that upgrades to WebSocket) goes through the filter chain, but once upgraded, frames bypass it. The official Spring pattern for STOMP auth is a `ChannelInterceptor` on the client inbound channel — exactly what this change does.

The two-layer defense (CORS origin + JWT) is intentional. Origin headers are advisory; JWTs prove identity. Together they make casual probing fail twice.

## Goals / Non-Goals

**Goals:**
- Every authenticated STOMP session carries a server-validated principal.
- Notifications are addressed per-user; no client ever subscribes to another user's data.
- Auth failures close the WS session cleanly with a STOMP ERROR frame.

**Non-Goals:**
- WS authorization rules per topic (we have one user-destination prefix; no fine-grained ACLs needed).
- Token refresh inside long-lived WS sessions (refresh handled by the client reconnecting with a fresh JWT — sky-view's responsibility).
- HMAC alternatives or session cookies. JWT only.

## Decisions

1. **STOMP `CONNECT` is the auth gate**, not the HTTP handshake. SockJS adds an HTTP layer; some envs let it through unauthenticated, others not. STOMP `CONNECT` is universal across transports.
2. **`oauth2-resource-server` over custom JWT parsing.** Standard, validates signature + claims correctly, integrates with `JwtDecoder` cache. Don't roll our own.
3. **`/user/queue/notify` per-user**, not `/topic/notify` broadcast. Spring's built-in user-destination machinery (`UserDestinationMessageHandler`) does the rewriting; we don't manage mappings.
4. **Principal name = JWT `sub` (or `email`)**. Spring's default `JwtAuthenticationToken.getName()` returns `sub`; if Sky's user identity is email, configure `JwtAuthenticationConverter` to use the `email` claim. Match what booking/offer/message services use as "user email" today (per `USER_INFO_HEADERS`).
5. **Feature flag** `sky.notify.ws.auth.enabled` — allows a brief rollout window where auth is enforced by the interceptor only in non-prod. Default `true` once the sky-view counterpart ships. Removable after one release.
6. **Audience claim validation**: configure `JwtDecoder` to require `aud=sky-api` (or whatever the Auth0 API identifier is). Defense against using a JWT minted for another service.

## Risks / Trade-offs

- **sky-view coordination**: this change breaks the frontend's WS connect until sky-view sends the JWT. Coordinate the deploy.
- **Long-lived sessions and token expiry**: a 24h JWT in a session that lasts 25h gets weird. Spring won't auto-disconnect; the client should reconnect periodically. Documented.
- **Auth0 issuer URL coupling**: the env var pins the Auth0 tenant. Worth abstracting? Not now — same coupling exists at the Ingress.
- **`/topic/notify` removal vs. retention**: if any current consumer subscribes to broadcast, removal breaks it. Current code does `convertAndSend("/notify", ...)`; nothing else subscribes by audit. Safe to remove `/topic` after switching.
- **WebSocket fallback to SockJS over HTTP**: still works; the JWT check happens at the STOMP layer regardless of transport.
- **Reactive vs. Servlet**: sky-notify is servlet-based (Spring MVC + WebSocket via JSR-356). No reactive concerns.
