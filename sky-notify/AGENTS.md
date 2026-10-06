# AGENTS.md: sky-notify

Module-local guidance for `sky-notify`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-notify` is the notifications service: it consumes Kafka events emitted by the other services and pushes them to
connected clients over WebSocket. It runs on port 5554 (`NOTIFY_PORT`, default 5554) and is one of the four
independently-deployable services. The module version is not restated here: the topmost `## [x.y.z]` entry in
[CHANGELOG.md](CHANGELOG.md) is the current one, and it wins over the cosmetic `version` in `build.gradle.kts`.

## Architecture

- Build plugin: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-notify.jar`, JaCoCo
  report). Depends on `:sky-common`. A build-script tweak wires `annotationProcessor` into `compileOnly` so Lombok and
  the Spring configuration processor cooperate.
- Hexagonal layout (`com.lukk.sky.notify`):
  - `domain/ports`, `domain/service`: the core (no `domain/model` package; this service is event-relay, not an entity
    store).
  - `adapters/inbound`: Kafka consumers; `adapters/outbound/websocket`: the WebSocket push adapter;
    `adapters/dto`: wire DTOs. That subpackage was called `adapters/outbound/service` until it was renamed for
    the transport it adapts, because the old name said neither a technology nor a concern and read as a second
    `domain.service` package.
  - `config`, `config/kafka`, `config/propertyBind`: Spring wiring and bound properties.
- Allowed browser origins are a bound property, not a list in the code. `WebSocketConfig` takes
  `sky.crossOrigin.allowed` through its constructor and splits it on commas, exactly as the `CorsConfig` of the three
  REST services does, and the value comes from `ACCESS_CONTROL_ALLOW_ORIGIN`. The Helm chart supplies it per
  environment: empty in `values.yaml` behind a `required`, so a render with no overlay fails and names the value,
  `https://sky.luksarna.com` in `values-prod.yaml` and the two local origins in `values-local.yaml`. No per-service
  port belongs on that list: in the cluster nothing publishes one so a browser can never originate there, and under
  compose a page calling its own service is same origin and never consults the list. The committed default in
  `application.yaml` names the production frontend and the two local origins, and it is a fallback for a run outside
  the chart rather than the value the cluster runs on.
- Stateless: no JPA, no database, no Flyway, and there is no `db/migration` directory and no JDBC driver on the
  classpath. Do not add a datastore without a design decision: this service holds no persistent state.
- Security on the STOMP channel, not on an HTTP request: all four services validate JWTs themselves through
  `ResourceServerJwtAutoConfiguration` in `sky-common`, so that is no longer what makes this module different. What is
  specific here is that the check runs on the STOMP CONNECT frame: `sky-notify` pulls
  `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` and `spring-security-messaging`, and
  `WebSocketAuthChannelInterceptor` validates the `Authorization` bearer token on CONNECT and sets the principal.
  `OAUTH2_ISSUER_URI` is the issuer for both paths and its name is deliberately provider-neutral: the realm this
  project ships is Keycloak, keep the variable name free of any provider.
- Per-user destination isolation is structural, not matcher-based: Spring Security's
  `MessageMatcherDelegatingAuthorizationManager` builder exposes no direct
  `simpSubscribeDestMatchers("/user/{principal}/**").hasUserPrincipal()` predicate, so `WebSocketConfig` gates
  `/user/**` subscriptions as `.authenticated()` and any connected (hence already-JWT-validated) user may subscribe to
  that namespace. Isolation comes from the push side instead: the server only sends to a user's own queue via
  `convertAndSendToUser(principal, ...)`, so no user reaches another user's notifications without knowing the other's
  principal name. Still missing, and worth adding when the framework allows it: a fine-grained per-principal subscribe
  guard, once Spring Security exposes a path-variable-to-principal matcher in the `SimpDestinationMessageMatcher` API.
- The dead-letter producer sets all five delivery-guarantee properties explicitly and leaves none to a client
  default. They live in `config/kafka/KafkaConsumerConfig.dltProducerFactory`, in Java rather than in
  `application.yaml`, because this module builds its own `ProducerFactory` from a map instead of using Spring
  Boot's auto-configured one: `ACKS_CONFIG` `all`, `ENABLE_IDEMPOTENCE_CONFIG` `true`, `RETRIES_CONFIG`
  `Integer.MAX_VALUE`, `DELIVERY_TIMEOUT_MS_CONFIG` 120000 and `MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION` 5, the
  last two as named constants rather than literals.
  Three of the five interact, and none of the numbers is a free choice:
  - `max.in.flight.requests.per.connection` is 5 because that is the ceiling the idempotent producer enforces.
    `ProducerConfig.postProcessAndValidateIdempotenceConfigs` in kafka-clients 4.1.2 throws
    `ConfigException("To use the idempotent producer, max.in.flight.requests.per.connection must be set to at most
    5")` above that, so 6 fails producer construction instead of misbehaving at run time. Dropping to 1 would cost
    throughput for nothing: the idempotent producer preserves order at any permitted value, which is what the
    client's own `enable.idempotence` documentation says.
  - `delivery.timeout.ms` is 120000 because the client requires it to be at least `linger.ms + request.timeout.ms`
    and `KafkaProducer.configureDeliveryTimeout` throws `ConfigException("delivery.timeout.ms should be equal to or
    larger than linger.ms + request.timeout.ms")` for an explicitly set value below that sum. Nothing in this
    repository sets either of those two, so their 4.1.2 defaults apply, 5 and 30000, and the floor is 30005. Note
    that `linger.ms` defaulted to 0 before Kafka 4.0, so the floor moved. Setting `linger.ms` or
    `request.timeout.ms` here means rechecking that sum.
  - `retries` is a pin rather than a guarantee, and saying so is the point. With `delivery.timeout.ms` set, that
    timeout is the real bound on how long a send is retried, and the client documentation says to leave `retries`
    unset and control retry behaviour through the timeout instead. The only behaviour the value still carries is
    that an idempotent producer refuses 0, so every non-zero number behaves identically and a large one is not
    tuning. It is set because the specification requires all five to be explicit and because a client default that
    moved to 0 would silently disable idempotence.
  All three of the values added here happen to equal the kafka-clients 4.1.2 default, which is exactly why they are
  written down: a default that moves between client versions must not be able to change the durability of a write
  without a line of this repository changing.
  Java expresses one thing the two YAML producers cannot: `Integer.MAX_VALUE` by name, where
  `application.yaml` in sky-booking and sky-offer has to carry the literal 2147483647. Nothing runs the other
  way: every one of the five is expressible in both places, so the three producers do not drift.
  `DltKafkaProducerDeliveryGuaranteeTest` asserts the resolved `ProducerFactory.getConfigurationProperties()`
  map the booted context builds, not the configuration source that feeds it, and builds a real `KafkaProducer`
  from that map so both validations above actually run. sky-booking and sky-offer carry the identical five
  values, so change all three or none.
- Messaging: consumes Kafka events via `spring-kafka`, deserialised with the Spring-managed Jackson 3
  `ObjectMapper` (`tools.jackson.databind.ObjectMapper`). Gson is gone from this module. No springdoc UI (no REST
  surface beyond the WebSocket handshake).

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit). No H2 and no database
container: this module has no datastore, and H2 is gone from the whole repository including the version catalogue.
Integration tests use Testcontainers Kafka only. Run from the repo root, e.g. `./gradlew :sky-notify:test`.

> Coverage note: the backfill landed. This module now carries a test class per production class, and the JaCoCo report
> shows no missed lines and no missed branches across the four classes it measures (`KafkaListeners`,
> `NotificationPublisherPrimary`, `NotificationTransmissionServicePrimary`, `WebSocketService`). `config/**` and
> `adapters/dto/**` are excluded from the report, though `WebSocketConfig`, `SecurityConfig` and
> `WebSocketAuthChannelInterceptor` are covered by their own tests anyway. `jacocoTestCoverageVerification` is wired
> into `check` for this module, at 0.90 line and 0.90 branch over that measured set, so the floor is enforcement rather
> than convention: a change that drops below it fails the build, so keep adding a test with every change here.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: `adapters/inbound` and `adapters/outbound` depend inward on `domain/ports`, never the
  reverse. ArchUnit enforces this.
- Two of those four rules landed in `HexagonalArchitectureTest` here rather than all four, and both were proved to
  fail on a deliberate violation before they were trusted. Every class under `domain` may reach only the packages an
  allow list in that test names, and that list is shorter than the one the three stateful services carry because this
  service stores nothing, so it holds no persistence, validation or Spring Data entry. Nothing may reach another
  service's classes. The repository rule and the `@RestControllerAdvice` rule are deliberately absent, because this
  module has neither, and a rule whose selector matches nothing reads as enforcement while checking nothing. A new
  framework dependency in the domain therefore fails the build: admit it in
  `openspec/specs/architecture/spec.md` through the change workflow first, and never by adding an exclusion to the
  test.
- Two further rules gate the shape of the outbound tree, and both were proved to fail on a deliberate violation before
  they were trusted. The class depending on `SimpMessagingTemplate` must live under `adapters.outbound.websocket`
  rather than anywhere under `adapters.outbound`, and no package under `adapters` may be named `service`, `impl` or
  `util`. The second rule is an approximation of the specification sentence it enforces, which asks for a subpackage
  named for what it adapts: a rule cannot judge whether a name describes a transport, so it forbids the names that
  describe nothing.
