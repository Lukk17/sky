# sky-notify

Real-time notification service for the Sky platform.

Port: 5554, transport STOMP over WebSocket, with a SockJS fallback.

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

The producer is configured for durability rather than for throughput, and all five delivery-guarantee properties are written down rather than inherited: `acks=all`, `enable.idempotence=true`, `retries=2147483647`, `delivery.timeout.ms=120000` and `max.in.flight.requests.per.connection=5`. Three of them constrain each other. The in-flight value is 5 because that is the most an idempotent producer is allowed, and kafka-clients 4.1.2 refuses to build a producer above it. The delivery timeout has to be at least `linger.ms + request.timeout.ms`, which is 30005 with the defaults this service leaves in place, and the client refuses an explicit value below that sum. `retries` is honestly a pin rather than a guarantee: once a delivery timeout is set, that timeout is what bounds retrying, and any non-zero retry count behaves the same. All three of those values equal the current client default, which is the reason they are written down at all: a default that moves between client versions must not be able to change the durability of a write silently. This module sets it in Java, in [src/main/java/com/lukk/sky/notify/config/kafka/KafkaConsumerConfig.java](src/main/java/com/lukk/sky/notify/config/kafka/KafkaConsumerConfig.java), because the dead-letter `ProducerFactory` is built by hand rather than auto-configured. `DltKafkaProducerDeliveryGuaranteeTest` asserts the configuration the booted context resolves. sky-booking and sky-offer carry the same five values.

---

### WebSocket endpoint

Clients connect at `/notifyWebsocket`, registered twice in
[src/main/java/com/lukk/sky/notify/config/WebSocketConfig.java](src/main/java/com/lukk/sky/notify/config/WebSocketConfig.java),
once with SockJS and once as a plain WebSocket. After connecting, subscribe to:

```text
/user/{email}/queue/notify
```

Spring's user-destination machinery resolves the `{email}` prefix from the authenticated STOMP principal. Clients
subscribe to `/user/queue/notify` and the broker rewrites it to the correct per-user destination automatically.

Allowed origins (CORS): `https://sky.luksarna.com`, `https://skycloud.luksarna.com`, `http://localhost:5777`,
`http://localhost:4200`. CORS is defence in depth here, a STOMP `CONNECT` needs a valid JWT regardless of origin.
`http://localhost:5777` covers a page served from the gateway port or from the local k3d ingress, and
`http://localhost:4200` covers the Angular dev server, so neither is refused at the origin check.

Reaching the endpoint works the same way in every environment. Locally, `sky-gateway` passes `/notifyWebsocket/**`
through to this service, so a client connects at `ws://localhost:5777/notifyWebsocket` through the gateway, or at
`ws://localhost:5554/notifyWebsocket` straight to the published port. In a cluster the Helm chart at
[../config/k8s/helm/service/sky-notify/](../config/k8s/helm/service/sky-notify/) templates an Ingress on
`/notifyWebsocket` with `pathType: Prefix` and no rewrite, so the same path reaches a browser: `localhost` with the
`dev-ssl-cert` secret locally, `skycloud.luksarna.com` with `sky-tls-cert` in production. The Ingress carries no
oauth2-proxy auth annotations, because the JWT check belongs on the STOMP `CONNECT` frame and not on the HTTP
handshake, and it raises `proxy-read-timeout` and `proxy-send-timeout` to 3600 seconds so nginx does not drop an idle
socket after its default 60.

---

### Authentication

All four services validate JWTs themselves through `ResourceServerJwtAutoConfiguration` in `sky-common`, so this one
is no longer the exception it used to be. What is specific here is where the check happens: not on an HTTP request but
on the STOMP CONNECT frame, which must carry a Bearer token in an `Authorization` header.
`WebSocketAuthChannelInterceptor` extracts and validates it with the `JwtDecoder` built from `OAUTH2_ISSUER_URI` and
sets the resulting principal on the session. Any STOMP frame without a valid JWT is rejected.

---

### Architecture

Uses hexagonal (ports-and-adapters):

- `domain/ports` and `domain/service`: the core. There is no `domain/model` package, this service relays events rather than owning entities.
- `adapters/inbound`: `KafkaListeners`, the Kafka consumers.
- `adapters/outbound`: the STOMP push through `SimpMessagingTemplate`.
- `adapters/dto`: `WebsocketPayloadModel`.
- `config`, `config/kafka`, `config/propertyBind`: Spring, security, and Kafka wiring.

---

### Key environment variables

| Variable | Notes |
|---|---|
| `OAUTH2_ISSUER_URI` | OIDC issuer, for example `https://keycloak.test:9443/realms/sky`, which is also the default |
| `OAUTH2_AUDIENCE` | Set to `sky-backend` to enforce the audience claim. Unset by default |
| `NOTIFY_PORT` | Default `5554` |
| `KAFKA_ADDRESS` | Default `kafka-service` |
| `KAFKA_PORT` | Default `9092` |

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-notify:test
```

Integration tests use a Testcontainers Kafka container only, wired through `@ServiceConnection`. There is no database container, this service has no datastore. Docker must be running.

The coverage backfill landed. The last JaCoCo run reports no missed lines and no missed branches at all, across `KafkaListeners`, `NotificationPublisherPrimary`, `NotificationTransmissionServicePrimary`, and `WebSocketService`. The report excludes `config/**`, `adapters/dto/**`, the application class, and `Constants`, so `WebSocketConfig`, `SecurityConfig`, and `WebSocketAuthChannelInterceptor` do not count towards that figure even though each has its own test class. `jacocoTestCoverageVerification` is wired into `check`, with a floor of 0.90 line and 0.90 branch over that measured set, so a drop below it fails the build here rather than waiting for a review to notice.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Platform overview, modules, build, ports |
| [AGENTS.md](AGENTS.md) | Module-local agent and coding conventions |
| [../config/local-dev/local_README.md](../config/local-dev/local_README.md) | Running the platform locally |
| [../sky-common/README.md](../sky-common/README.md) | The shared Kafka envelope and security auto-configurations this service uses |
