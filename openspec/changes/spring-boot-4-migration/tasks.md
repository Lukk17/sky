## 1. Toolchain prep

- [ ] 1.1 Install JDK 25 locally (developers). Document via `.tool-versions` (asdf), `mise.toml`, or README.
- [ ] 1.2 Update CI workflow `setup-java` step to `java-version: 25`, `distribution: temurin`.
- [ ] 1.3 Update `gradle/libs.versions.toml` `java = "25"`. Update `sky.java-conventions` toolchain to `JavaLanguageVersion.of(25)`.
- [ ] 1.4 Confirm `./gradlew --version` reports JDK 25.

## 2. Bump Spring Boot & related

- [ ] 2.1 `libs.versions.toml`: bump `spring-boot` to 4.0.x (pick the current patch at apply time), `springdoc` to the SB4-compatible release (3.x line per springdoc).
- [ ] 2.2 Update `spring-dependency-management` plugin if a newer compatible version exists.
- [ ] 2.3 Bump JUnit Jupiter, Mockito (if separately pinned), AssertJ to whatever SB4 BOM aligns with.

## 3. Code-level fixes

- [ ] 3.1 Replace `@MockBean` with `@MockitoBean` and import `org.springframework.test.context.bean.override.mockito.MockitoBean` in the 5 test files identified.
- [ ] 3.2 Audit for any `WebMvcConfigurerAdapter`, `WebSecurityConfigurerAdapter`, deprecated `spring.factories` — expected zero hits (verified in audit).
- [ ] 3.3 Audit `RestClient` calls for SB4 signature shifts; adjust if needed.
- [ ] 3.4 Audit Spring Security 7 changes in sky-notify (`SecurityFilterChain` builder may have signature shifts).
- [ ] 3.5 Audit Spring Kafka 4 changes — `DefaultErrorHandler`, `Acknowledgment` package, `Listener` factory.

## 4. Docker base images

- [ ] 4.1 In `docker-modernization`'s rewritten Dockerfiles, switch base images to `gradle:8.x-jdk25` and `eclipse-temurin:25-jre-alpine` (verify Alpine 25 availability at apply time; fall back to non-alpine).
- [ ] 4.2 If JDK 25 base images are not yet on Alpine, use Debian-slim variants.

## 5. Tests

- [ ] 5.1 `./gradlew test` — run the full suite (now including the additions from `test-modernization`). All green.
- [ ] 5.2 Smoke-test each service locally end-to-end (boot offer, send message, notify WS).
- [ ] 5.3 Confirm Testcontainers MySQL + Kafka images still work on JDK 25 runtime.

## 6. Verify

- [ ] 6.1 `./gradlew bootRun` per service starts on JDK 25 with Spring Boot 4 banner.
- [ ] 6.2 No `deprecation` warnings in compile output (or only intentionally-deferred ones).
- [ ] 6.3 Helm deploy: pull new images, all pods reach Ready.
- [ ] 6.4 Postman/Newman E2E green.
- [ ] 6.5 Update root `AGENTS.md` ## Architecture section: Spring Boot 4, Java 25.
