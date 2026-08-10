# sky-notify

*Real-time notification service for the Sky platform.*

Port: **5554** | Transport: WebSocket (STOMP over SockJS)

---

### What it does

`sky-notify` is the event relay. It consumes Kafka events published by `sky-offer` and `sky-booking`, then pushes
each event as a STOMP message to the connected browser session belonging to the user who triggered the action.

This service is stateless: no database, no JPA, no Flyway. It holds no persistent state.

---

### Kafka topics consumed

| Topic | Produced by | What triggers it |
|---|---|---|
| `offerTopic-1` | `sky-offer` | Offer created, edited, or deleted |
| `bookingTopic-1` | `sky-booking` | Booking created or deleted |

Both listeners are in `KafkaListeners`. Failed messages are retried via `FixedBackOff` and routed to a dead-letter
topic (`<topic>.DLT`) after exhausting retries.

---

### WebSocket endpoint

Clients connect at `/notifyWebsocket` using STOMP over SockJS. After connecting, subscribe to:

```
/user/{email}/queue/notify
```

Spring's user-destination machinery resolves the `{email}` prefix from the authenticated STOMP principal. Clients
subscribe to `/user/queue/notify` and the broker rewrites it to the correct per-user destination automatically.

Allowed origins (CORS): `https://sky.luksarna.com`, `https://skycloud.luksarna.com`, `http://localhost:4200`.

---

### Authentication

`sky-notify` validates JWTs itself, unlike the other three services that rely entirely on the `oauth2-proxy` ingress.
The STOMP CONNECT frame must carry a Bearer token in an `Authorization` header. `WebSocketAuthChannelInterceptor`
extracts and validates the token using the `JwtDecoder` built from `OAUTH2_ISSUER_URI`. Any STOMP frame without a
valid JWT is rejected.

---

### Architecture

Uses hexagonal (ports-and-adapters):

- `domain/ports`, `domain/service`: core. `NotificationTransmissionService` is the primary port.
- `adapters/inbound`: `KafkaListeners` (Kafka consumers).
- `adapters/outbound`: `WebSocketService` (STOMP push via `SimpMessagingTemplate`), `NotificationPublisherPrimary`.
- `adapters/dto`: `WebsocketPayloadModel`.
- `config`, `config/kafka`, `config/propertyBind`: Spring, security, and Kafka wiring.

---

### Key environment variables

| Variable | Notes |
|---|---|
| `OAUTH2_ISSUER_URI` | OIDC issuer URI, e.g. `https://lukk17.eu.auth0.com/`. Required; no default. |
| `NOTIFY_PORT` | Default `5554` |
| `KAFKA_ADDRESS` | Default `kafka-service` |
| `KAFKA_PORT` | Default `9092` |

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-notify:test
```

Integration tests use Testcontainers Kafka only (no MySQL). Docker must be running.

This module has low test coverage. The JaCoCo gate is intentionally not wired into `check` yet. When changing code
here, add tests.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Root README: full platform overview, build, deployment |
| [AGENTS.md](AGENTS.md) | Module-local agent/coding conventions |
