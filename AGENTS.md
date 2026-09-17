# AGENTS.md

This file provides shared instructions to all AI coding agents working in this repository (Claude Code, Kilo Code, OpenCode, Codex CLI, GitHub Copilot). Standards and skills are imported from [agent-standards](https://github.com/Lukk17/agent-standards).

## Skills

This project includes agent skills in `.agents/skills/`. Invoke relevant skills before starting implementation work. Examples:

- `/code-reviewer` before reviewing code
- `/security-review` before auditing for vulnerabilities
- `/coding-standards` before writing new code
- `/tdd-workflow` before adding features or fixing bugs

Slash commands may appear as `/name` or `/name.md` in your agent's autocomplete. Use whichever your agent shows.

## Working With Agents

All supported agents read this `AGENTS.md` from the project root and auto-discover skills from `.agents/skills/`. Start your agent from the project root:

- Claude Code: run `claude`. Reads `.claude/CLAUDE.md`, which imports this file. Hooks and permissions in [.claude/settings.json](.claude/settings.json).
- Kilo Code: reads `AGENTS.md` automatically. Takes its MCP servers and its preflight plugin from [opencode.json](opencode.json), which Kilo accepts as a legacy project config filename, and its subagents from `.kilo/agents`.
- OpenCode: reads `AGENTS.md` automatically. [opencode.json](opencode.json) at the project root declares the MCP servers and the shared hook plugin.
- Codex CLI: run `codex`. Reads `AGENTS.md` automatically. Project MCP servers and hook tables in [.codex/config.toml](.codex/config.toml), subagents in `.codex/agents/`. Codex loads project-scoped `.codex/` layers (config, hooks, rules) only for a project you have marked trusted, so an untrusted checkout silently runs on the global `~/.codex/config.toml` alone. This project is marked trusted, so the layer loads. One manual step is still outstanding: Codex records trust per hook, keyed on that hook's own hash, and a project-level trust entry does not cover them. Until someone runs `/hooks` once in an interactive Codex session started from the repository root, all five hook tables in [.codex/config.toml](.codex/config.toml) (`UserPromptSubmit`, `SessionStart`, `Stop`, `SubagentStart`, `PreToolUse`) stay skipped, which means no preflight gate and no task-list sync under Codex. Editing a hook's command changes its hash, so the approval is needed again after any such edit.
- GitHub Copilot: reads `AGENTS.md`, `.agents/skills/` and `.github/agents/` natively. MCP in [.vscode/mcp.json](.vscode/mcp.json) for VS Code and [.github/mcp.json](.github/mcp.json) for the CLI, hooks in [.github/hooks/preflight.json](.github/hooks/preflight.json).

## OpenSpec Workflow

This project uses [OpenSpec](https://github.com/Fission-AI/OpenSpec) for spec-driven development. Specs and changes live under `openspec/`.

The full lifecycle (run inside your agent shell):

1. Propose a change. Agent generates proposal, design, and `tasks.md` under `openspec/changes/`:
   ```text
   /opsx:propose add dark mode support
   ```
2. Apply the code. After reviewing/editing `tasks.md`, agent implements and checks off tasks:
   ```text
   /opsx:apply
   ```
3. Verify and refine. Pass back logs or bug reports to refine:
   ```text
   /opsx:verify The toggle button is invisible on mobile. Fix it.
   ```
4. Archive. Once tested, merge delta specs into `openspec/specs/` and archive the change folder:
   ```text
   /opsx:archive
   ```

Some agents render commands as `/opsx-propose.md` instead of `/opsx:propose`. Both work, use what appears in your autocomplete.

Use multiline prompts when you need to include logs or detailed context with a command.

## What This Repo Is

`sky` is a Spring Boot microservices backend for a flight/offer booking platform. It exposes a REST API consumed by the [Sky-View](https://github.com/Lukk17/sky-view) frontend, and is deployed to a Kubernetes cluster on GCP at https://skycloud.luksarna.com/. The repo is a multi-module Gradle (Kotlin DSL) monorepo with four independently-deployable services and a local-development gateway:

- `sky-booking` (port 5555): bookings on offers
- `sky-offer` (port 5552): offer CRUD
- `sky-message` (port 5553): user-to-user messaging
- `sky-notify` (port 5554): push notifications to clients
- `sky-gateway` (port 5777): local-development edge proxy, not deployed to the cluster

Shared build configuration lives in the `buildSrc/` convention plugins (`sky.java-conventions`, `sky.java-library-conventions`, `sky.spring-service-conventions`, `sky.web-conventions`, `sky.kafka-conventions`) with all versions pinned in the `gradle/libs.versions.toml` version catalogue. Helm charts, local-dev compose, and deployment scripts live under `config/`. The Bruno API collection lives under `docs/api/request/`.

Two more modules complete the build. `sky-common` is a shared library (not a deployable service): it holds wire types, Spring auto-configurations, security defaults, and web utilities consumed by the other five modules, `sky-gateway` included. `sky-gateway` is a Spring Cloud Gateway edge proxy: it reproduces the production ingress path rewrites so the whole stack answers on one port under docker-compose, and it builds a container image but ships no Helm chart. Each module also carries its own `AGENTS.md` with module-local detail. See [Module guides](#module-guides) below.

## Architecture

- Stack: Java 25, Spring Boot 4.0.7 (Spring Framework 7), Spring Web (MVC) in all four services, Spring Cloud 2025.1.2 with Spring Cloud Gateway on WebFlux in `sky-gateway`, Spring Data JPA, Spring Kafka 4, Flyway 11, springdoc-openapi 3.0 (Swagger UI at `/swagger-ui/index.html`), Lombok 1.18.38, JUnit 5.11, Gradle Kotlin DSL.
- Code structure: hexagonal / ports & adapters per service. Within `domain/`, the layout is:
  `domain/model` (entities), `domain/exception`, `domain/ports/inbound` (driving port interfaces called by controllers),
  `domain/ports/outbound` (driven port interfaces implemented by infrastructure adapters: JPA repositories, Kafka
  publishers, REST clients, S3 storage), and `domain/service` (domain service implementations and internal
  interfaces). The I/O layer lives in `adapters/inbound` (REST controllers, Kafka listeners) and `adapters/outbound`
  (`rest` clients, `notification` publishers, `storage`), with `adapters/dto` for wire types.
  The `archunit` test dependency enforces the dependency direction and the controller location.
- API: REST under a `/api/v1` URL prefix (`apiPrefix`), stripped at the ingress. Earlier `hello` probe endpoints were dropped. Every error answers `application/problem+json`, with no second shape anywhere: `UnhandledExceptionResolver` in `sky-common` catches whatever no advice mapped and answers a 500 problem detail that names no exception, no message and no class, so Spring Boot's flat `{timestamp, status, error, path}` body no longer reaches a client. It is a `HandlerExceptionResolver` at `Ordered.LOWEST_PRECEDENCE` rather than a `@ControllerAdvice`, because an advice cannot sort behind the service advices and a catch-all that wins would turn every 503, 502, 409 and 404 into a 500. See [sky-common/AGENTS.md](sky-common/AGENTS.md) before touching it.
- Persistence: PostgreSQL (`org.postgresql:postgresql` at runtime, with `flyway-database-postgresql`) for the three stateful services (`sky-booking`, `sky-offer`, `sky-message`) against a shared `sky` database whose tables all live in the `public` schema, versioned with Flyway (`src/main/resources/db/migration/` per service). Tests run against Testcontainers (`postgres:17-alpine` plus Kafka) wired by `@ServiceConnection`, aligned across all three modules. The deployed database in the Helm charts and the local-dev container is still `postgres:16-alpine`. H2 is gone from every module and from the version catalogue. `sky-notify` is stateless (no JPA, no DB).
- Inter-service comms: REST plus Kafka (`spring-kafka`) for async events, with `sky-notify` consuming events and pushing to clients over WebSocket. The only synchronous service-to-service hop is `sky-booking` calling `sky-offer` to resolve an offer's owner before it accepts a booking. A dependency outage is 503 with `Retry-After: 10` everywhere in this repository, and `sky-booking` is where that shape is implemented, on the one hop it has: connection refused, a connect timeout, a read timeout, a 5xx after the retries are exhausted, and an open circuit breaker all answer 503 with that header and an RFC 9457 problem detail. A missing offer stays 404, any other 4xx from `sky-offer` is 502, and 400 is reserved for a request that really is malformed. Never fold a dependency outage back into a 400, and never let a 404 count as a circuit-breaker failure: the breaker records only `ResourceAccessException` and `IOException`, so a run of lookups for offers that do not exist cannot open it. `sky-offer` now carries the same shape on the one dependency it has, its object store: a refused connection, a connect or read timeout, a 5xx and a 429 all answer 503 with that header, a store that answers 401, 403 or a missing bucket is 502, and a file the service itself rejects stays 400. `sky-message` makes no outbound call of any kind. It has no Kafka, no REST client and no `adapters/outbound` package at all, and its only contact with Keycloak is fetching the signing keys to validate a token, exactly like the other three services.
- Auth / edge: `oauth2-proxy` (OIDC against Keycloak, `provider: keycloak-oidc`, realm `sky`) and routing are handled in the Kubernetes ingress layer. Keep the three edge components distinct by name: `nginx-ingress` is the ingress, `oauth2-proxy` is the authentication proxy, and `sky-gateway` is the local-development gateway. None of them is a substitute for another. All four services also validate JWTs themselves via `spring-boot-starter-oauth2-resource-server`, wired by `ResourceServerJwtAutoConfiguration` in `sky-common` against a provider-neutral `OAUTH2_ISSUER_URI`. There is no basic auth and no username or password in any local profile: under the `local` profile `LocalSecurityAutoConfiguration` in `sky-common` registers `UnverifiedJwtDecoder`, so the same JWT chain accepts any well-formed token without verifying signature, issuer or expiry, and `sky-gateway`, being reactive and outside that servlet auto-configuration, carries its own `local` filter chain that permits every exchange with basic auth and form login explicitly disabled.
- Packaging: one fat `bootJar` per service, containerized via per-module `docker/Dockerfile`, deployed as Helm charts under `config/k8s/helm/` (per-service charts under `service/`, plus `api-gateway/` for `oauth2-proxy` and sealed-secrets, `db/` for PostgreSQL, `kafka/`, and `infra/` for Keycloak and floci, the S3-compatible object store).
- Build: single composite Gradle build (one root `gradlew`, modules wired in `settings.gradle.kts`, shared config in `buildSrc/` convention plugins, versions in `gradle/libs.versions.toml`). The build runs on Gradle 9 with the daemon on JDK 25 (pinned in `gradle/gradle-daemon-jvm.properties`), and the Java 25 compile toolchain is auto-resolved via the Foojay plugin. The catalogue targets Java 25 / Spring Boot 4.
- Active work: branch `feature/spring-boot-4-and-arch-cleanup` (current), Spring Boot 4 / JDK 25 readiness and architecture cleanup. There is no in-flight proposal right now: `openspec/changes/` holds only `archive/`, and the merged specs live in `openspec/specs/`.
- Testing: JUnit 5 + Spring Boot Test, `spring-kafka-test`, `okhttp3 mockwebserver`, Spring Security Test, Testcontainers (PostgreSQL and Kafka) for repository and integration tests, `archunit` for architecture rules. E2E lives in `e2e/` (OpenSpec runbooks driven through the Bruno collection in `docs/api/request/`).
- Coverage: `sky.jacoco-conventions` is applied by `sky.java-conventions`, so every module inherits it, the `sky-common` library included. It filters the measured class set down to hand-written logic, excluding `**/dto/**`, `**/config/**`, `**/*Application.class` and `**/Constants.class`, and wires `jacocoTestCoverageVerification` into `check` at 0.90 line and 0.90 branch, so `./gradlew build` fails a module that drops below either. The HTML and XML report and the gate both set their `classDirectories` from that one filter on purpose: reporting one set and enforcing another is how a gate ends up enforcing a number nobody agreed to. The plugin names no module, so a module whose measured set comes out empty opts itself out by disabling the verification task in its own `build.gradle.kts`, and `sky-gateway` is the only one that does, because its whole main source set is `config/SecurityConfig` plus the application class and a rule over an empty counter passes for nothing measured. Do not add a module-name condition to the shared plugin.
- Constraints: monorepo with shared `group = com.lukk`; one root `gradlew` for the whole composite build; versions are pinned centrally in `gradle/libs.versions.toml` (bump there, not per-module).

## Required opening move

Before any code work, name the skill(s) and subagent(s) that own the task and invoke them, or state that none apply and why. Default to delegating investigation, review, and bounded implementation rather than doing everything inline. Four wirings carry this gate: the `UserPromptSubmit` and `PreToolUse` hooks in [.claude/settings.json](.claude/settings.json) (Claude Code), the inline `[[hooks.*]]` tables in [.codex/config.toml](.codex/config.toml) (Codex CLI), the shared runner [.agents/plugin/hooks.js](.agents/plugin/hooks.js) declared by the `plugin` array in [opencode.json](opencode.json) (OpenCode and Kilo Code, where it blocks the tool call rather than injecting text), and [.github/hooks/preflight.json](.github/hooks/preflight.json) (GitHub Copilot, once per session and on subagent start). All four quote this section, which is the canonical text: keep them in sync.

## Subagents

Subagents are specialised agents the main session delegates to. One canonical file per agent in the agent-standards repo fans out to four generated trees: [.agents/agents/](.agents/agents/) in OpenCode markdown, [.claude/agents/](.claude/agents/) in Claude markdown, [.codex/agents/](.codex/agents/) as TOML, and [.github/agents/](.github/agents/) as `name.agent.md` files. `.opencode/agents` and `.kilo/agents` are symlinks into `.agents/agents`, because OpenCode and Kilo Code read the same format and both follow symlinks when scanning an agent directory. These files are generated artifacts: do not hand-edit them. Change the canonical source in the agent-standards repo and re-import (see [docs/AGENT_TOOLING.md](docs/AGENT_TOOLING.md)).

### When to use them

Spawn subagents proactively, not reactively, and run them in parallel when the work is independent. Match the work pattern to the agent and tell each one which skill to invoke:

- Reviewing code → `code-reviewer` (with the `/code-reviewer` skill), plus `security-auditor` for auth/payment/sensitive-data changes.
- Adding a feature or fixing a bug → `java-pro` for implementation (driving the `/springboot-patterns` skill, whose `references/testing.md` carries the Spring Boot TDD workflow, plus `/java-coding-standards`), `backend-architect` first when a new service or API contract is involved.
- Debugging a failure → `debugger` for a single root cause, `error-detective` for cross-service log/trace patterns.
- Migrations / dependency upgrades (e.g. the Spring Boot 4 / JDK 25 work) → `legacy-modernizer`, phased with tests added before each refactor.
- Database / schema / query work → `database-expert` (with `/springboot-patterns`, whose `references/jpa.md` holds the JPA rules).
- Docs → `docs-architect` or `api-documenter`.

Spec-driven work still starts with an OpenSpec proposal (`/opsx:propose`) before delegating implementation.

## Module guides

Each module carries its own `AGENTS.md` with its port, dependencies, data story, and module-local conventions:

- [sky-common/AGENTS.md](sky-common/AGENTS.md): shared library (wire types, auto-configs, web utilities)
- [sky-booking/AGENTS.md](sky-booking/AGENTS.md): bookings service (port 5555)
- [sky-offer/AGENTS.md](sky-offer/AGENTS.md): offers service (port 5552)
- [sky-message/AGENTS.md](sky-message/AGENTS.md): messaging service (port 5553)
- [sky-notify/AGENTS.md](sky-notify/AGENTS.md): notifications service (port 5554)
- [sky-gateway/AGENTS.md](sky-gateway/AGENTS.md): local-development edge proxy (port 5777)