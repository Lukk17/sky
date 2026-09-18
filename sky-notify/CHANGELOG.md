# Changelog: sky-notify

All notable changes to this module are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to
[Semantic Versioning](https://semver.org/). Newest entry on top. The topmost `## [x.y.z]`
version is the current one. The release workflow reads it for the image tag and to guard
against re-publishing an already-released version, so keep it at the top and bump it before
every release. The `version` in `build.gradle.kts` is a cosmetic label the release workflow
does not read. If the two disagree, this file wins for release purposes.

## [2.0.0]

Major. The WebSocket handshake now requires a token and the subscribe destination moved, so a
client built against 1.x connects and then receives nothing. The JaCoCo coverage gate is wired
into `check` for this module, at 0.90 line and 0.90 branch over the measured class set, so add
tests when changing code here or the build fails.

### Changed
- A STOMP CONNECT without a valid bearer token is rejected. 1.x had no authentication on the
  WebSocket at all: any browser that passed the origin check could connect. The handshake now
  carries `Authorization: Bearer <token>` as a native STOMP header, the token is validated
  against `OAUTH2_ISSUER_URI`, its audience is checked, and the resulting principal is attached
  to the session. A missing or malformed header and an invalid token both fail the CONNECT.
- Notifications go to one user, not to everyone. 1.x sent every notification to the shared
  `/notify` broker destination, so every connected client received every other user's booking
  and offer events, including the payload. The server now sends to
  `/user/{principal}/queue/notify` via the user-destination machinery, and a client subscribes
  to `/user/queue/notify`. Subscribing to the old `/notify` destination returns nothing, which
  is the visible half of this break. The isolation is structural: the server only ever addresses
  a user's own queue, so there is no destination a client can subscribe to in order to read
  somebody else's notifications.
- The allowed browser origins are configuration rather than a list compiled into
  `WebSocketConfig`. The value binds from `sky.crossOrigin.allowed`, overridden by
  `ACCESS_CONTROL_ALLOW_ORIGIN`, which is the same property and the same variable the three REST
  services already read, and the Helm chart sets it per environment. A deployed 1.x and an early
  2.0.0 build accepted `http://localhost:5777` and `http://localhost:4200` in the cluster, because
  the list was fixed at compile time and no chart value could change it. The committed default
  names the production frontend and those two local origins for a run outside the chart.
- Kafka offsets are committed by hand after the notification has been delivered, not
  automatically on poll. 1.x ran with auto-commit on, so a consumer that died between the poll
  and the push lost those notifications permanently. The container is on
  `MANUAL_IMMEDIATE` acknowledgement and the listener acknowledges in a `finally` block after
  the transmission returns.
- JSON is Jackson 3 (`tools.jackson`), not Gson. `KafkaPayloadModel` comes from sky-common now,
  so the consumer and the two producers parse and emit one definition of the wire type rather
  than three copies that had already drifted.
- Spring Boot 4.0.7 on Java 25, built by Gradle 9.6.1 as a module of the composite build. The
  per-module wrapper, `settings.gradle.kts` and Gradle directory are gone: build from the
  repository root with `./gradlew :sky-notify:test`. Dependency versions come from
  `gradle/libs.versions.toml` and shared build logic from the `buildSrc` convention plugins.

### Added
- Dead letter routing. A message the listener cannot handle is retried three times after the
  first attempt, one second apart, then published to `<topic>.DLT` through an idempotent,
  `acks=all` producer, so it leaves the main partition instead of blocking it. A payload that
  will never parse
  (`JacksonException`, `IllegalArgumentException`) is not retried at all and goes straight to the
  dead letter topic. 1.x had no error handler, so one malformed message stalled the partition
  behind it indefinitely. The dead-letter producer now sets all five delivery-guarantee
  properties explicitly and leaves none to a client default: `acks=all`,
  `enable.idempotence=true`, `retries=Integer.MAX_VALUE`, `delivery.timeout.ms=120000` and
  `max.in.flight.requests.per.connection=5`. The three that were missing all equal the current
  kafka-clients default, which is exactly why they are written down: a default that moves
  between client versions must not be able to change the durability of a write without a line of
  this repository changing. `retries` is a pin rather than a guarantee, because the delivery
  timeout is what actually bounds retrying, and the module documentation says so rather than
  implying a large number is tuning.
- `DltKafkaProducerDeliveryGuaranteeTest`, which asserts the five properties on the resolved
  `ProducerFactory.getConfigurationProperties()` map the booted context builds rather than on the
  Java source that feeds it, and builds a real `KafkaProducer` from that map so the two client
  startup validations run: the idempotent producer's in-flight ceiling of 5, and the requirement
  that `delivery.timeout.ms` be at least `linger.ms + request.timeout.ms`.
- Per-frame authorization through `@EnableWebSocketSecurity`, so every frame after the CONNECT
  is checked rather than only the handshake. Subscriptions to `/user/**` and sends to `/sky/**`
  both require an authenticated session.
- Correlation id propagation from the Kafka record header into the consumer thread, so a
  notification can be followed back to the HTTP request in sky-booking or sky-offer that caused
  it, and is cleared afterwards so it cannot leak into the next record on a pooled thread.
- Consumer concurrency is configurable through `sky.kafka.consumer.concurrency`, defaulting to 1.
- Hexagonal layout with the dependency direction enforced by ArchUnit: `adapters/inbound` and
  `adapters/outbound` depend inward on `domain/ports` and never the reverse.
- Testcontainers Kafka integration tests, an ArchUnit layering suite, and unit coverage over the
  audience validator.
- A local profile that starts without Keycloak, a per-service startup banner, and a structured
  startup log line naming the deployment it thinks it is in.

### Removed
- The module copy of `KafkaPayloadModel` and the three `propertyBind` classes, all of which now
  come from sky-common.

### Fixed
- A poison pill message took the consumer group down with it. With no error handler and
  auto-commit on, a record that failed to parse was retried forever without the offset ever
  advancing, so every later notification on that partition was never delivered. The dead letter
  route plus manual acknowledgement means a bad record is moved aside and the partition keeps
  moving.
- A token valid for a different client was accepted on the strength of its signature alone,
  because nothing checked the audience. `AudienceValidator` now rejects a token whose `aud` claim
  does not name this backend.
