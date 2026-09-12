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
  - `adapters/inbound`: Kafka consumers; `adapters/outbound`: WebSocket push; `adapters/dto`: wire DTOs.
  - `config`, `config/kafka`, `config/propertyBind`: Spring wiring and bound properties.
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
