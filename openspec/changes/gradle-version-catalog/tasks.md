## 1. Inventory current versions

- [ ] 1.1 Read `config/microservicesConfig.gradle.kts` and list every `extra["xVersion"]` entry with its value.
- [ ] 1.2 Read each service `build.gradle.kts` and list every string-literal version (plugins, dependencies).
- [ ] 1.3 Read each service `settings.gradle.kts` for any plugin-management versions.

## 2. Create the catalog

- [ ] 2.1 Create `gradle/libs.versions.toml` with `[versions]`, `[libraries]`, `[plugins]`, `[bundles]` sections.
- [ ] 2.2 Populate `[versions]` with: `spring-boot`, `spring-dependency-management`, `java`, `lombok`, `junit-jupiter`, `springdoc`, `mockwebserver`.
- [ ] 2.3 Populate `[libraries]` with Spring starters used across services, lombok, jaxb api/runtime, gson, mysql-connector, springdoc starters, h2, spring-security-test, junit-jupiter, spring-kafka-test, mockwebserver.
- [ ] 2.4 Populate `[plugins]` with `org.springframework.boot` and `io.spring.dependency-management`.
- [ ] 2.5 Define `[bundles]`: `spring-web-stack`, `kafka`, `test-spring`, `test-kafka`.

## 3. Wire it in

- [ ] 3.1 In root `settings.gradle.kts` (or each service's until multi-project lands), enable the default `libs` catalog (Gradle picks up `gradle/libs.versions.toml` automatically with no extra config).
- [ ] 3.2 Replace plugin block in every `build.gradle.kts` with `alias(libs.plugins.spring.boot)` / `alias(libs.plugins.spring.dependency.management)`.
- [ ] 3.3 Replace every `implementation("group:art")` with `implementation(libs.<accessor>)` or bundle reference.
- [ ] 3.4 Delete `extra[...]` blocks from `config/microservicesConfig.gradle.kts`.

## 4. Verify

- [ ] 4.1 `./gradlew :sky-booking:dependencies --configuration runtimeClasspath` — every dep resolves with catalog version.
- [ ] 4.2 Repeat for sky-offer, sky-message, sky-notify.
- [ ] 4.3 `./gradlew build` across all four services — passes.
- [ ] 4.4 No string-literal versions remain. Grep: `grep -rn '"[0-9]\+\.[0-9]\+\.[0-9]\+' --include='build.gradle.kts'` returns only `version = "1.0.X"` of the service itself.
