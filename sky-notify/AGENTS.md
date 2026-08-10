# AGENTS.md — sky-notify

Module-local guidance for `sky-notify`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules; this file only adds what is specific to this module.

## What This Module Is

`sky-notify` is the notifications service: it consumes Kafka events emitted by the other services and pushes them to
connected clients over **WebSocket**. It runs on **port 5554** (`NOTIFY_PORT`, default 5554) and is one of the four
independently-deployable services. `version = "1.0.2"`.

## Architecture

- **Build plugin**: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-notify.jar`, JaCoCo
  report). Depends on `:sky-common`. A build-script tweak wires `annotationProcessor` into `compileOnly` so Lombok and
  the Spring configuration processor cooperate.
- **Hexagonal layout** (`com.lukk.sky.notify`):
  - `domain/ports`, `domain/service` — the core (no `domain/model` package; this service is event-relay, not an entity
    store).
  - `adapters/inbound` — Kafka consumers; `adapters/outbound` — WebSocket push; `adapters/dto` — wire DTOs.
  - `config`, `config/kafka`, `config/propertyBind` — Spring wiring and bound properties.
- **Stateless**: no JPA, no MySQL, no Flyway — there is no `db/migration` directory. Do not add a datastore without a
  design decision; this service holds no persistent state.
- **Security is here, not just at the edge**: `sky-notify` pulls `spring-boot-starter-security` and
  `spring-boot-starter-oauth2-resource-server`, and validates JWTs itself. The `JwtDecoder` is built from a
  provider-neutral `OAUTH2_ISSUER_URI` (works against Keycloak, Auth0, or any OIDC IdP) — keep that env var name
  provider-agnostic. The other three services rely on the `oauth2-proxy` gateway instead.
- **Messaging**: consumes Kafka events via `spring-kafka`; Gson for JSON payloads. No springdoc UI (no REST surface
  beyond the WebSocket handshake).

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit, H2). Integration tests use
**Testcontainers Kafka** only. Run from the repo root, e.g. `./gradlew :sky-notify:test`.

> Coverage note: this module is the lowest-covered service (the JaCoCo audit found ~2 tests across 15+ production
> classes, ~6% branch coverage). The coverage gate is intentionally **not** wired into `check` yet, precisely so it does
> not block PRs until the `sky-notify` test backfill lands. When adding or changing code here, add tests — this is the
> module that most needs them.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: `adapters/inbound` and `adapters/outbound` depend inward on `domain/ports`, never the
  reverse — ArchUnit enforces this.
