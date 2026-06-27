# AGENTS.md — sky-message

Module-local guidance for `sky-message`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules; this file only adds what is specific to this module.

## What This Module Is

`sky-message` is the messaging service: user-to-user messages. It runs on **port 5553** (`MESSAGE_PORT`, default 5553),
exposes REST under the `/api/v1` prefix, and is one of the four independently-deployable services. `version = "1.0.2"`.

## Architecture

- **Build plugin**: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-message.jar`, JaCoCo
  report). Depends on `:sky-common`.
- **Hexagonal layout** (`com.lukk.sky.message`):
  - `domain/model`, `domain/exception` — core model and domain exceptions.
  - `domain/ports/inbound` — driving port interfaces called by controllers (`MessageService`).
  - `domain/ports/outbound` — driven port interfaces implemented by infrastructure adapters (`MessageRepository`).
  - `domain/service` — domain service implementations (`MessageServicePrimary`).
  - `adapters/inbound/api` — REST controllers; `adapters/dto` — wire DTOs.
  - `config`, `config/propertyBind` — Spring wiring and bound properties.
- **MVC stack**: plain Spring Web (`spring-boot-starter-web`).
- **No Kafka**: unlike the other three services, `sky-message` does **not** depend on `spring-kafka`. It has no
  `config/kafka` package and no `adapters/notification`. Do not add Kafka here without a deliberate design decision —
  message delivery is synchronous REST today.
- **Persistence**: MySQL (`mysql-connector-j`, runtime) against the shared `sky` schema, schema versioned with Flyway
  (`src/main/resources/db/migration/V1__init.sql`). H2 in tests.
- **API docs**: springdoc `webmvc` UI at `/swagger-ui/index.html`.

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit, H2). Integration tests use
**Testcontainers MySQL** only (no Kafka container). Run from the repo root, e.g. `./gradlew :sky-message:test`.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: adapters depend inward on `domain/ports/inbound` and `domain/ports/outbound`;
  `domain/service` implementations depend on those port interfaces. Adapters must never import from `domain/service`
  directly — ArchUnit enforces this.
