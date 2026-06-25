## ADDED Requirements

### Requirement: All services run on Spring Boot 4 and Java 25
Every service MUST compile and run on Spring Boot 4 (Spring Framework 7) and Java 25. Deprecated SB3 test annotations (`@MockBean`) MUST be replaced by their SB4 equivalents (`@MockitoBean`).

#### Scenario: Starting a service
- **WHEN** a developer runs `./gradlew :sky-booking:bootRun`
- **THEN** the Spring Boot banner prints version `4.x.y`; `System.getProperty("java.version")` reports `25.x.y`

#### Scenario: Test compilation
- **WHEN** the build compiles tests across all services
- **THEN** no source file imports `org.springframework.boot.test.mock.mockito.MockBean`; all tests use `org.springframework.test.context.bean.override.mockito.MockitoBean`

### Requirement: Framework version is catalog-managed
The Spring Boot version MUST be declared in `gradle/libs.versions.toml`. Bumping the framework MUST be a single TOML edit (modulo any required code-level deprecation fixes).

#### Scenario: Verifying single source of truth
- **WHEN** searching the repo for the Spring Boot version string
- **THEN** the only authoritative declaration is in `gradle/libs.versions.toml` under `[versions]`
