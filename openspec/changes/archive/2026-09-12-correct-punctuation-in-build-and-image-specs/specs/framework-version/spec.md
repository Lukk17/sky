## MODIFIED Requirements

### Requirement: All services run on Spring Boot 4 and Java 25
Every module MUST compile and run on Spring Boot 4 (Spring Framework 7) and Java 25, currently pinned as `spring-boot = "4.0.7"` and `java = "25"` in `gradle/libs.versions.toml`. No module may pin its own framework version or its own language level: the Java toolchain is resolved once from the catalogue's `java` entry in the shared convention plugin, so a module cannot drift onto a different language level without editing the catalogue. The Spring Boot 3 test annotation `@MockBean` MUST NOT appear in any source file, and a test that overrides a bean MUST use `@MockitoBean` instead.

#### Scenario: Starting a service
- **WHEN** a developer runs `./gradlew :sky-booking:bootRun`
- **THEN** the Spring Boot banner prints a 4.x version and the JVM running the service reports a Java 25 runtime

#### Scenario: Test compilation
- **WHEN** the build compiles tests across all modules
- **THEN** no source file imports `org.springframework.boot.test.mock.mockito.MockBean`, and every test bean override imports `org.springframework.test.context.bean.override.mockito.MockitoBean` instead
