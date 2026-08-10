# gradle-build Specification

## Purpose
TBD - created by archiving change gradle-version-catalog. Update Purpose after archive.
## Requirements
### Requirement: Single version catalog owns every dependency version
The repository MUST use a Gradle version catalog at `gradle/libs.versions.toml` as the sole declaration site for dependency and plugin versions. Build scripts MUST reference catalog accessors (`libs.*`) rather than string-literal versions.

#### Scenario: Bumping a shared dependency is a one-line edit
- **WHEN** a contributor needs to upgrade Spring Boot, JUnit, springdoc, Lombok, mockwebserver, or any catalog-managed dependency
- **THEN** the only edit required is changing the version value in `gradle/libs.versions.toml`, and every service picks up the new version on the next build

#### Scenario: No literal version strings linger in build files
- **WHEN** the catalog is in place
- **THEN** grepping `build.gradle.kts` files for `"\d+\.\d+\.\d+"` returns only the per-service `version = "x.y.z"` field (and zero dependency or plugin literals)

### Requirement: Single rooted Gradle multi-project build
The repository MUST be a single Gradle multi-project rooted at the repo root, with one `gradlew` wrapper, one `settings.gradle.kts` listing every service module, and shared configuration delivered via convention plugins under `buildSrc/`.

#### Scenario: Building all services from the root
- **WHEN** a contributor runs `./gradlew build` from the repository root
- **THEN** every service (`sky-booking`, `sky-offer`, `sky-message`, `sky-notify`, and `sky-common`) builds and its tests run

#### Scenario: One wrapper exists
- **WHEN** searching the repository for `gradlew` files
- **THEN** exactly one wrapper exists at the repo root (per-service wrappers are removed)

### Requirement: Convention plugins eliminate build-script duplication
Common Spring service configuration (Java toolchain, Spring Boot plugin, dependency-management, actuator, lombok, configuration-processor, JAX-B, h2, junit, spring-security-test) MUST be expressed as reusable convention plugins under `buildSrc/`, not duplicated across each service's `build.gradle.kts`.

#### Scenario: Adding a common dependency to every Spring service
- **WHEN** the team decides every Spring service needs a new shared starter
- **THEN** the change is made once in `sky.spring-service-conventions.gradle.kts` and applies to every service that consumes that plugin

