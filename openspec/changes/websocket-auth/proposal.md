## Why

The sky-notify WebSocket endpoint (`/notifyWebsocket`, STOMP over SockJS) authenticates nothing on the handshake. CORS restricts origins to three known frontends, but origin headers are easily spoofed outside a browser, and any *real* browser on those origins can subscribe to the broadcast topic `/topic/notify` and read every other user's notifications. The audit calls this critical.

The fix has two parts:
1. **Authenticate the STOMP `CONNECT` frame** via a `ChannelInterceptor` that validates an Authorization header (JWT from the same Auth0 flow the REST API uses behind the Ingress).
2. **Route notifications per-user**, not broadcast — using `convertAndSendToUser(user, "/queue/notify", message)` and `/user/{user}/queue/notify` subscriptions. Then even if auth failed open, a user could only ever see their own messages.

This pairs with `kafka-reliability` (consumer must ack only after the per-user emit succeeds; we already get this from that change).

## What Changes

- **Add** Spring Security to sky-notify (`spring-boot-starter-security`).
- **Add** JWT validation: `spring-boot-starter-oauth2-resource-server`. Issuer URI from Auth0 (env var, matches what Ingress validates).
- **Add** `WebSocketAuthChannelInterceptor` that:
  - Intercepts `CONNECT` frames.
  - Reads `Authorization: Bearer <jwt>` from STOMP headers.
  - Validates JWT (signature, expiry, issuer, audience).
  - Sets `Principal` on the `Message<?>` so downstream `SimpMessageHeaderAccessor.getUser()` returns the authenticated user.
- **Register** the interceptor in `WebSocketConfig.configureClientInboundChannel(...)`.
- **Switch** emission from `convertAndSend("/notify", msg)` to `convertAndSendToUser(targetUser, "/notify", msg)`. Determine `targetUser` from the Kafka payload (the existing payload model includes user email from the `KafkaPayloadModel` headers).
- **Adjust** STOMP destination prefixes: keep `/notify` as a user destination prefix; client subscribes to `/user/notify` (Spring rewrites to `/user/<principal>/notify` internally).
- **Update** sky-view (the frontend, separate repo) connection URL to include the JWT — out of scope here, but documented in the change-notes.

## Capabilities

### New Capabilities
- `websocket-auth`: STOMP `CONNECT` requires a valid JWT; the principal is set on inbound messages; emission is per-user, never broadcast.

### Modified Capabilities
- _None inside this repo._ Service surface unchanged; only the auth contract tightens.

## Impact

- **Touched files**: sky-notify `build.gradle.kts` (security deps), new `WebSocketAuthChannelInterceptor`, edits to `WebSocketConfig`, `WebSocketService`, `NotificationTransmissionServicePrimary`, `NotificationPublisherPrimary` (now needs a user ID), `application.yml` (Auth0 issuer URI).
- **Client breaking change**: existing sky-view WebSocket subscriptions stop working until updated. Coordinate with sky-view repo.
- **CORS origin whitelist** stays as defense-in-depth.
- **Risk**: medium-high. Auth failures could lock all WS clients out during rollout. Mitigation: feature flag (`sky.notify.ws.auth.enabled`) for a brief overlap window. Default `true` after smoke test.
- **Dependency order**: depends on `kafka-reliability` (we want manual ack so failed per-user delivery doesn't lose the message). Independent of `spring-boot-4-migration` (Spring Security 6 already works; SB4 brings Spring Security 7 which is additive here).
