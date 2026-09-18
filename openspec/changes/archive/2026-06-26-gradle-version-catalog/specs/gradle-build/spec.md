## ADDED Requirements

### Requirement: Single version catalog owns every dependency version
The repository MUST use a Gradle version catalog at `gradle/libs.versions.toml` as the sole declaration site for dependency and plugin versions. Build scripts MUST reference catalog accessors (`libs.*`) rather than string-literal versions.

#### Scenario: Bumping a shared dependency is a one-line edit
- **WHEN** a contributor needs to upgrade Spring Boot, JUnit, springdoc, Lombok, mockwebserver, or any catalog-managed dependency
- **THEN** the only edit required is changing the version value in `gradle/libs.versions.toml`, and every service picks up the new version on the next build

#### Scenario: No literal version strings linger in build files
- **WHEN** the catalog is in place
- **THEN** grepping `build.gradle.kts` files for `"\d+\.\d+\.\d+"` returns only the per-service `version = "x.y.z"` field (and zero dependency or plugin literals)
