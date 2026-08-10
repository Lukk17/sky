## Why

Dependency versions are scattered across `config/microservicesConfig.gradle.kts` (`extra` properties), string literals inside each service's `build.gradle.kts`, and inherited defaults from the Spring Boot BOM. Bumping one shared dep (springdoc, JUnit, Spring Boot) requires editing 1–5 files in lockstep and tends to drift. Gradle's version catalog (`gradle/libs.versions.toml`) is the modern, type-safe replacement: one file, type-safe accessors (`libs.spring.boot`), and IDE autocomplete.

## What Changes

- **Add** `gradle/libs.versions.toml` at repo root with `[versions]`, `[libraries]`, `[plugins]`, `[bundles]` sections.
- **Migrate** every literal version string and `extra["xVersion"]` reference into the catalog. Concrete inventory:
  - `springBootVersion = "3.5.4"` (later bumped by `spring-boot-4-migration`)
  - `javaVersion = "VERSION_21"`
  - `lombokVersion = "1.18.38"`
  - `jUnit5Version = "5.11.0"`
  - `openapiVersion = "2.6.0"`
  - `mockwebserverVersion = "4.12.0"`
  - `dependency-management = "1.1.7"` (currently a plugin literal in every service)
- **Define bundles** for `spring-web-stack`, `kafka`, `test-spring`, `test-kafka` to deduplicate the most-repeated dependency groups.
- **Replace** all `implementation("group:art:literal")` and `version` references in all four `build.gradle.kts` and `config/microservicesConfig.gradle.kts` with `libs.*` accessors.

## Capabilities

### New Capabilities
- `gradle-version-catalog`: Single TOML file owns every dependency version and plugin version in the repo; build scripts reference accessors only.

### Modified Capabilities
- _None._ Pure structural refactor; no behavior change.

## Impact

- **Touched files**: `settings.gradle.kts` (root, after `gradle-multi-project` lands; until then, each service's `settings.gradle.kts`), all four `build.gradle.kts`, `config/microservicesConfig.gradle.kts`, new `gradle/libs.versions.toml`.
- **CI**: none. Gradle 8.14.3 already supports version catalogs natively.
- **Risk**: low — version catalog is opt-in syntactic sugar; any error fails the build loudly at configure time, not at runtime.
- **Dependency order**: prefer to land *before* `gradle-multi-project` so the multi-project setup can apply the catalog from day one, but can also land after. Must land before `spring-boot-4-migration` so the SB4 version bump is a single TOML edit.
