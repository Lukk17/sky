# AGENTS.md — sky-offer

Module-local guidance for `sky-offer`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules; this file only adds what is specific to this module.

## What This Module Is

`sky-offer` is the offers service: CRUD over flight/booking offers, the inventory the other services book against. It runs
on **port 5552** (`OFFER_PORT`, default 5552), exposes REST under the `/api/v1` prefix, and is one of the four
independently-deployable services. `version = "1.0.2"`.

## Architecture

- **Build plugin**: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-offer.jar`, JaCoCo
  report). Depends on `:sky-common`.
- **Hexagonal layout** (`com.lukk.sky.offer`):
  - `domain/model`, `domain/ports`, `domain/exception` — the core.
  - `adapters/api` — REST controllers; `adapters/dto` — wire DTOs; `adapters/notification` — outbound notification.
  - `config`, `config/kafka`, `config/propertyBind` — Spring wiring and bound properties.
- **MVC stack**: plain Spring Web (`spring-boot-starter-web`); no WebFlux here (unlike `sky-booking`).
- **Persistence**: MySQL (`mysql-connector-j`, runtime) against the shared `sky` schema, schema versioned with Flyway
  (`src/main/resources/db/migration/V1__init.sql`). H2 in tests.
- **Messaging**: produces/consumes Kafka events via `spring-kafka`; Gson for JSON payloads.
- **API docs**: springdoc `webmvc` UI at `/swagger-ui/index.html`.

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit, H2). Integration tests use
**Testcontainers MySQL + Kafka** (`@ServiceConnection`). Run from the repo root, e.g. `./gradlew :sky-offer:test`.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: adapters depend inward on `domain/ports`, never the reverse — ArchUnit enforces this.
