# AGENTS.md — sky-booking

Module-local guidance for `sky-booking`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules; this file only adds what is specific to this module.

## What This Module Is

`sky-booking` is the bookings service: it manages bookings placed against offers. It runs on **port 5555**
(`BOOKING_PORT`, default 5555), exposes REST under the `/api/v1` prefix, and is one of the four independently-deployable
services. `version = "1.0.2"`.

## Architecture

- **Build plugin**: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-booking.jar`, JaCoCo
  report). Depends on `:sky-common`.
- **Hexagonal layout** (`com.lukk.sky.booking`):
  - `domain/model`, `domain/ports`, `domain/exception` — the core; `BookingService` is exposed as a port.
  - `adapters/api` — REST controllers; `adapters/dto` — wire DTOs; `adapters/notification` — outbound notification.
  - `config`, `config/kafka`, `config/propertyBind` — Spring wiring and bound properties.
- **Reactive stack**: this service keeps WebFlux. `BookingService` returns `Mono<BookingDTO>` and a `RestClientWebflux`
  uses `WebClient` for inter-service calls. Migrating from `WebClient` to `RestClient` is a separate, deliberate change —
  do not silently swap it.
- **Persistence**: MySQL (`mysql-connector-j`, runtime) against the shared `sky` schema, schema versioned with Flyway
  (`src/main/resources/db/migration/V1__init.sql`). H2 in tests.
- **Messaging**: produces/consumes Kafka events via `spring-kafka`; Gson for JSON payloads.
- **API docs**: springdoc `webmvc` UI at `/swagger-ui/index.html`.

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit, H2). Integration tests use
**Testcontainers MySQL + Kafka** (`@ServiceConnection`, no `@DynamicPropertySource`). `spring-kafka-test` and
`okhttp3 mockwebserver` cover messaging and outbound HTTP. Run from the repo root with the single composite `./gradlew`,
e.g. `./gradlew :sky-booking:test`.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: controllers and adapters depend inward on `domain/ports`, never the reverse. ArchUnit
  enforces this — a violation fails the build, do not weaken the rule.
