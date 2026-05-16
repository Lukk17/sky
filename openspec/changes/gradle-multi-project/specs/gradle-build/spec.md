## ADDED Requirements

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
