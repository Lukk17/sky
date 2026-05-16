## 1. Bootstrap the root project

- [ ] 1.1 Create root `settings.gradle.kts` with `rootProject.name = "sky"`, plus `include(":sky-booking", ":sky-offer", ":sky-message", ":sky-notify")`.
- [ ] 1.2 Wire up version catalog in root `settings.gradle.kts` (default `libs` from `gradle/libs.versions.toml`).
- [ ] 1.3 Create root `build.gradle.kts` with `group`, top-level repositories block, and shared `allprojects` / `subprojects` config if needed (prefer convention plugins).
- [ ] 1.4 Copy one service's `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties` to repo root. Use Gradle 8.14.3.

## 2. Convention plugins

- [ ] 2.1 Create `buildSrc/build.gradle.kts` declaring `kotlin-dsl` plugin and Gradle plugin marker deps for Spring Boot + dependency-management (so the convention plugins can apply them).
- [ ] 2.2 Create `buildSrc/src/main/kotlin/sky.java-conventions.gradle.kts`: applies `java`, sets toolchain to Java 21 from catalog, declares Maven Central repository.
- [ ] 2.3 Create `buildSrc/src/main/kotlin/sky.spring-service-conventions.gradle.kts`: applies `java-conventions`, applies spring-boot + dependency-management plugins, adds actuator + configuration-processor + lombok + jaxb + h2 + spring-security-test + junit-jupiter dependencies via catalog bundles, configures `tasks.test { useJUnitPlatform() }` and `bootJar { archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}") }`.
- [ ] 2.4 Create `buildSrc/src/main/kotlin/sky.kafka-conventions.gradle.kts`: adds spring-kafka + spring-kafka-test.
- [ ] 2.5 Create `buildSrc/src/main/kotlin/sky.web-conventions.gradle.kts`: adds spring-boot-starter-web, validation, springdoc-webmvc-ui.

## 3. Migrate each service

- [ ] 3.1 sky-booking: replace `build.gradle.kts` with `plugins { id("sky.spring-service-conventions"); id("sky.kafka-conventions"); id("sky.web-conventions") }` + JPA + mysql + gson + mockwebserver test dep + service-specific bits. Delete its `gradlew`, `gradlew.bat`, `gradle/`, `settings.gradle.kts`.
- [ ] 3.2 Repeat for sky-offer (JPA + kafka + web).
- [ ] 3.3 Repeat for sky-message (JPA + web; no kafka).
- [ ] 3.4 Repeat for sky-notify (websocket starter + kafka conventions; no JPA, no web).
- [ ] 3.5 Delete `config/microservicesConfig.gradle.kts` once nothing references it.

## 4. Update external references

- [ ] 4.1 Update each `*/docker/Dockerfile` build context to repo root, invoke `./gradlew :sky-<name>:bootJar`. Coordinate with `docker-modernization` change.
- [ ] 4.2 Update `config/k8s/_deployment-scripts/` shell/bat scripts that call gradle.
- [ ] 4.3 Update root README's build instructions.

## 5. Verify

- [ ] 5.1 From repo root: `./gradlew build` — all services build.
- [ ] 5.2 `./gradlew :sky-booking:test :sky-offer:test :sky-message:test :sky-notify:test` — all tests pass.
- [ ] 5.3 `./gradlew :sky-booking:bootJar` produces the same artifact as before (jar name + main class).
- [ ] 5.4 `find . -name gradlew` returns only `./gradlew` (no per-service wrappers left).
