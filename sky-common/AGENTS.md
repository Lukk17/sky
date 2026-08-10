# AGENTS.md — sky-common

Module-local guidance for `sky-common`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules; this file only adds what is specific to this module.

## What This Module Is

`sky-common` is a **shared library**, not a deployable service. It has no `main` class, no `bootJar`, and no port. The
four services (`sky-booking`, `sky-offer`, `sky-message`, `sky-notify`) depend on it via `implementation(project(":sky-common"))`
to share wire types, Spring auto-configurations, and web utilities.

Current `version = "1.0.2"`; `description = "sky-common — shared wire types, auto-configurations, and web utilities"`.

## Architecture

- **Build plugin**: `sky.java-library-conventions` (a plain Java library — no Spring Boot plugin, no `bootJar`, no JaCoCo
  service gate), in contrast to the `sky.spring-service-conventions` the four services use.
- **Package layout** (`com.lukk.sky.common`):
  - `common.web` — shared web utilities (e.g. exception-handler base, HTTP header constants).
  - `common.kafka` — `KafkaProducerAutoConfiguration` and related messaging auto-config.
- **Dependency discipline**: Spring Web, Spring MVC, Jakarta Validation, SLF4J, Spring Kafka, and
  `spring-boot-autoconfigure` are all declared **`compileOnly`** on purpose. A consumer that is not a web service (or not
  a Kafka service) must not pull Spring MVC or `spring-kafka` transitively just by depending on `sky-common`. Tests
  re-add the real dependencies via `testImplementation`.

## Conventions

- When adding a shared type, keep the `compileOnly` rule: if the new code needs a runtime dependency, declare it
  `compileOnly` here and let each consuming service opt in with its own `implementation` entry. Do not promote a
  `compileOnly` dependency to `implementation` without checking every consumer.
- Auto-configurations are registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  A new auto-config class must be added there to take effect in consumers.
- This is a library: there is no application profile, no `application.yaml`, and nothing to run standalone. Verify changes
  through the consuming services' tests.
