# websocket Specification

## Purpose
TBD - created by archiving change websocket-auth. Update Purpose after archive.
## Requirements
### Requirement: STOMP CONNECT requires a valid JWT
The sky-notify service MUST reject any STOMP `CONNECT` frame that lacks a valid JWT in the `Authorization` header. A `ChannelInterceptor` on the client-inbound channel MUST validate the token (signature, expiry, issuer, audience) using the same Auth0 issuer the REST API trusts.

#### Scenario: Connecting with a valid token
- **WHEN** a client opens a STOMP connection with `Authorization: Bearer <valid-jwt>`
- **THEN** the `CONNECT` succeeds; subsequent frames carry the authenticated principal

#### Scenario: Connecting without a token
- **WHEN** a client opens a STOMP connection with no `Authorization` header
- **THEN** the server returns a STOMP `ERROR` frame and closes the session

#### Scenario: Connecting with an expired or invalid token
- **WHEN** the JWT is expired, has the wrong audience, or fails signature validation
- **THEN** the server returns a STOMP `ERROR` frame and closes the session

### Requirement: Notifications are delivered per-user, not broadcast
Server-initiated WebSocket messages MUST go through `convertAndSendToUser(user, "/queue/notify", payload)`. Clients MUST subscribe to `/user/queue/notify` to receive their own messages. The broadcast destination `/topic/notify` MUST NOT carry user-specific notifications.

#### Scenario: One user's notification does not leak to another
- **WHEN** sky-offer publishes an event for user A on `offerTopic-1`, and clients for both user A and user B are subscribed to `/user/queue/notify`
- **THEN** only user A's client receives the message

