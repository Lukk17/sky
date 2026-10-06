## 1. Toolchain prep

- [x] 1.1 Install JDK 25 locally (developers). Document via `.tool-versions` (asdf), `mise.toml`, or README.
- [x] 1.2 Update CI workflow `setup-java` step to `java-version: 25`, `distribution: temurin`.
- [x] 1.3 Update `gradle/libs.versions.toml` `java = "25"`. Update `sky.java-conventions` toolchain to `JavaLanguageVersion.of(25)`.
- [x] 1.4 Confirm `./gradlew --version` reports JDK 25.

## 2. Bump Spring Boot & related

- [x] 2.1 `libs.versions.toml`: bump `spring-boot` to 4.0.x (pick the current patch at apply time), `springdoc` to the SB4-compatible release (3.x line per springdoc).
- [x] 2.2 Update `spring-dependency-management` plugin if a newer compatible version exists.
- [x] 2.3 Bump JUnit Jupiter, Mockito (if separately pinned), AssertJ to whatever SB4 BOM aligns with.

## 3. Code-level fixes

- [x] 3.1 Replace `@MockBean` with `@MockitoBean` and import `org.springframework.test.context.bean.override.mockito.MockitoBean` in the 5 test files identified.
- [x] 3.2 Audit for any `WebMvcConfigurerAdapter`, `WebSecurityConfigurerAdapter`, deprecated `spring.factories` — expected zero hits (verified in audit).
- [x] 3.3 Audit `RestClient` calls for SB4 signature shifts; adjust if needed.
- [x] 3.4 Audit Spring Security 7 changes in sky-notify (`SecurityFilterChain` builder may have signature shifts).
- [x] 3.5 Audit Spring Kafka 4 changes — `DefaultErrorHandler`, `Acknowledgment` package, `Listener` factory.

## 4. Docker base images

- [x] 4.1 In `docker-modernization`'s rewritten Dockerfiles, switch base images to `gradle:8.x-jdk25` and `eclipse-temurin:25-jre-alpine` (verify Alpine 25 availability at apply time; fall back to non-alpine).
- [x] 4.2 If JDK 25 base images are not yet on Alpine, use Debian-slim variants.

## 5. Tests

- [x] 5.1 `./gradlew test` — run the full suite (now including the additions from `test-modernization`). All green.
- [x] 5.2 Smoke-test each service locally end-to-end (boot offer, send message, notify WS).
- [x] 5.3 Confirm Testcontainers MySQL + Kafka images still work on JDK 25 runtime.

## 6. Verify

- [x] 6.1 `./gradlew bootRun` per service starts on JDK 25 with Spring Boot 4 banner.
- [x] 6.2 No `deprecation` warnings in compile output (or only intentionally-deferred ones).
- [x] 6.3 Helm deploy: pull new images, all pods reach Ready.
- [x] 6.4 Bruno E2E green.
- [x] 6.5 Update root `AGENTS.md` ## Architecture section: Spring Boot 4, Java 25.
