# AGENTS.md

This file provides shared instructions to all AI coding agents working in this repository (Claude Code, Kilo Code, OpenCode, Codex CLI). Standards and skills are imported from [agent-standards](https://github.com/Lukk17/agent-standards).

## Skills

This project includes agent skills in `.agents/skills/`. Invoke relevant skills before starting implementation work. Examples:

- `/code-reviewer` before reviewing code
- `/security-review` before auditing for vulnerabilities
- `/coding-standards` before writing new code
- `/tdd-workflow` before adding features or fixing bugs

Slash commands may appear as `/name` or `/name.md` in your agent's autocomplete — use whichever your agent shows.

## Working With Agents

All supported agents read this `AGENTS.md` from the project root and auto-discover skills from `.agents/skills/`. Start your agent from the project root:

- **Claude Code** — run `claude`. Reads `.claude/CLAUDE.md`, which imports this file.
- **Kilo Code** — reads `AGENTS.md` automatically. Optional `kilo.jsonc` for extra config.
- **OpenCode** — reads `AGENTS.md` automatically. Optional `opencode.json` at project root.
- **Codex CLI** — run `codex`. Reads `AGENTS.md` automatically. Global settings in `~/.codex/config.toml`.

## OpenSpec Workflow

This project uses [OpenSpec](https://github.com/Fission-AI/OpenSpec) for spec-driven development. Specs and changes live under `openspec/`.

The full lifecycle (run inside your agent shell):

1. **Propose a change** — agent generates proposal, design, and `tasks.md` under `openspec/changes/`:
   ```text
   /opsx:propose add dark mode support
   ```
2. **Apply the code** — after reviewing/editing `tasks.md`, agent implements and checks off tasks:
   ```text
   /opsx:apply
   ```
3. **Verify and refine** — pass back logs or bug reports to refine:
   ```text
   /opsx:verify The toggle button is invisible on mobile. Fix it.
   ```
4. **Archive** — once tested, merge delta specs into `openspec/specs/` and archive the change folder:
   ```text
   /opsx:archive
   ```

Some agents render commands as `/opsx-propose.md` instead of `/opsx:propose` — both work; use what appears in your autocomplete.

Use multiline prompts when you need to include logs or detailed context with a command.

## What This Repo Is

`sky` is a Spring Boot microservices backend for a flight/offer booking platform. It exposes a REST API consumed by the [Sky-View](https://github.com/Lukk17/sky-view) frontend, and is deployed to a Kubernetes cluster on GCP at https://skycloud.luksarna.com/. The repo is a multi-module Gradle (Kotlin DSL) monorepo with four independently-deployable services:

- `sky-booking` (port 5555) — bookings on offers
- `sky-offer` (port 5552) — offer CRUD
- `sky-message` (port 5553) — user-to-user messaging
- `sky-notify` (port 5554) — push notifications to clients

Shared build configuration lives in the `buildSrc/` convention plugins (`sky.java-conventions`, `sky.java-library-conventions`, `sky.spring-service-conventions`) with all versions pinned in the `gradle/libs.versions.toml` version catalogue. Helm charts, local-dev compose, deployment scripts, and a Postman E2E collection live under `config/`.

A fifth module, `sky-common`, is a shared library (not a deployable service): it holds wire types, Spring auto-configurations, and web utilities consumed by the four services. Each module also carries its own `AGENTS.md` with module-local detail — see [Module guides](#module-guides) below.

## Architecture

- **Stack**: Java 25, Spring Boot 4.0.7 (Spring Framework 7), Spring Web + WebFlux, Spring Data JPA, Spring Kafka 4, Flyway 11, springdoc-openapi 3.0 (Swagger UI at `/swagger-ui/index.html`), Lombok 1.18.38, JUnit 5.11, Gradle Kotlin DSL.
- **Code structure**: hexagonal / ports & adapters per service — `domain/model` and `domain/ports` hold the core; `adapters/api`, `adapters/dto`, `adapters/notification` (and `adapters/inbound` / `adapters/outbound` in `sky-notify`) hold the I/O. The `archunit` test dependency enforces the dependency direction.
- **API**: REST under a `/api/v1` URL prefix (`apiPrefix`), stripped at the ingress. Earlier `hello` probe endpoints were dropped.
- **Persistence**: MySQL (`mysql-connector-j`) for the three stateful services (`sky-booking`, `sky-offer`, `sky-message`) against a shared `sky` schema, schema versioned with Flyway (`V1__init.sql` per service); H2 in tests, with Testcontainers (MySQL + Kafka) for integration tests. `sky-notify` is stateless (no JPA, no DB).
- **Inter-service comms**: REST + Kafka (`spring-kafka`) for async events; `sky-notify` consumes events and pushes to clients over WebSocket.
- **Auth / edge**: an `oauth2-proxy` gateway (OIDC against Auth0, `lukk17.eu.auth0.com`) and routing are handled in the Kubernetes ingress layer, not inside most services. `sky-notify` additionally validates JWTs itself via `spring-boot-starter-oauth2-resource-server` against a provider-neutral `OAUTH2_ISSUER_URI`. Local profiles use Spring Security basic auth.
- **Packaging**: one fat `bootJar` per service, containerized via per-module `docker/Dockerfile`, deployed as Helm charts under `config/k8s/helm/` (per-service charts, plus `oauth2-proxy`, MySQL, Kafka, and sealed-secrets infra charts).
- **Build**: single composite Gradle build — one root `gradlew`, modules wired in `settings.gradle.kts`, shared config in `buildSrc/` convention plugins, versions in `gradle/libs.versions.toml`. The Gradle daemon runs on JDK 21 (pinned in `gradle/gradle-daemon-jvm.properties`) while the Java 25 compile toolchain is auto-resolved via the Foojay plugin; the catalogue targets Java 25 / Spring Boot 4.
- **Active work**: branch `feature/spring-boot-4-and-arch-cleanup` (current) — Spring Boot 4 / JDK 25 readiness and architecture cleanup; see `openspec/changes/` for in-flight proposals.
- **Testing**: JUnit 5 + Spring Boot Test, `spring-kafka-test`, `okhttp3 mockwebserver`, Spring Security Test, Testcontainers, H2 for repository tests, `archunit` for architecture rules. E2E lives in `config/postman-collection/`.
- **Constraints**: monorepo with shared `group = com.lukk`; one root `gradlew` for the whole composite build; versions are pinned centrally in `gradle/libs.versions.toml` — bump there, not per-module.

## Required opening move

Before any code work, name the skill(s) and subagent(s) that own the task and invoke them, or state that none apply and why. Default to delegating investigation, review, and bounded implementation rather than doing everything inline. This gate is re-injected every turn by the `UserPromptSubmit` preflight hook in [.claude/settings.json](.claude/settings.json) (Claude Code), the `.opencode/plugin/preflight.js` plugin (OpenCode), and `.kilocode/rules/00-preflight.md` (Kilo Code); Codex CLI relies on this section alone. Keep all four wordings in sync — this section is the canonical text.

## Subagents

Subagents are specialised agents the main session delegates to. They live in [.claude/agents/](.claude/agents/) and [.opencode/agents/](.opencode/agents/); Kilo Code reads `.opencode/agents/` natively, so OpenCode and Kilo share that directory. Codex CLI has no per-agent file mechanism and leans on `AGENTS.md` plus skills only. These files are **generated artifacts** — do not hand-edit them; change the canonical source in the agent-standards repo and re-import (see [docs/AGENT_TOOLING.md](docs/AGENT_TOOLING.md)).

### When to use them

Spawn subagents proactively, not reactively, and run them in parallel when the work is independent. Match the work pattern to the agent and tell each one which skill to invoke:

- **Reviewing code** → `code-reviewer` (with the `/code-reviewer` skill), plus `security-auditor` for auth/payment/sensitive-data changes.
- **Adding a feature or fixing a bug** → `java-pro` for implementation (driving the `/springboot-tdd` and `/java-coding-standards` skills), `backend-architect` first when a new service or API contract is involved.
- **Debugging a failure** → `debugger` for a single root cause, `error-detective` for cross-service log/trace patterns.
- **Migrations / dependency upgrades** (e.g. the Spring Boot 4 / JDK 25 work) → `legacy-modernizer`, phased with tests added before each refactor.
- **Database / schema / query work** → `database-expert` (with `/jpa-patterns`).
- **Docs** → `docs-architect` or `api-documenter`.

Spec-driven work still starts with an OpenSpec proposal (`/opsx:propose`) before delegating implementation.

## Module guides

Each module carries its own `AGENTS.md` with its port, dependencies, data story, and module-local conventions:

- [sky-common/AGENTS.md](sky-common/AGENTS.md) — shared library (wire types, auto-configs, web utilities)
- [sky-booking/AGENTS.md](sky-booking/AGENTS.md) — bookings service (port 5555)
- [sky-offer/AGENTS.md](sky-offer/AGENTS.md) — offers service (port 5552)
- [sky-message/AGENTS.md](sky-message/AGENTS.md) — messaging service (port 5553)
- [sky-notify/AGENTS.md](sky-notify/AGENTS.md) — notifications service (port 5554)