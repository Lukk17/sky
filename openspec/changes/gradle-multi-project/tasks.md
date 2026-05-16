## 1. Bootstrap the root project

- [x] 1.1 Root `settings.gradle.kts` with `rootProject.name = "sky"` and `include(":sky-booking", ":sky-offer", ":sky-message", ":sky-notify")`.
- [x] 1.2 Catalog auto-discovered from `gradle/libs.versions.toml` (no explicit `versionCatalogs` block needed at root).
- [x] 1.3 Root `build.gradle.kts` with `allprojects { group = "com.lukk" }` and a `printVersions` helper task.
- [x] 1.4 Copied sky-booking's `gradlew`, `gradlew.bat`, and `gradle/wrapper/` to repo root (Gradle 8.14.3).

## 2. Convention plugins

- [x] 2.1 `buildSrc/build.gradle.kts` with `kotlin-dsl`, Spring Boot Gradle plugin + dependency-management plugin marker deps, plus the `LibrariesForLibs` exposure hack so `libs` resolves inside precompiled `.gradle.kts` plugins.
- [x] 2.2 `buildSrc/src/main/kotlin/sky.java-conventions.gradle.kts` — Java 21 source/target, Maven Central, `useJUnitPlatform()`.
- [x] 2.3 `buildSrc/src/main/kotlin/sky.spring-service-conventions.gradle.kts` — applies java-conventions + spring-boot + dependency-management; adds actuator + devtools + lombok + configuration-processor + spring-boot-starter-test (junit excluded) + h2 + spring-security-test + junit-jupiter; sets `bootJar { archiveFileName }`.
- [ ] 2.4 `sky.kafka-conventions.gradle.kts` — not split out yet (kafka deps stay per-service for clarity; only 3 of 4 services use kafka). Revisit when extending the catalog with additional kafka libs in `kafka-reliability`.
- [ ] 2.5 `sky.web-conventions.gradle.kts` — same reasoning; web/webflux split per service for now.

## 3. Migrate each service

- [x] 3.1 sky-booking: `plugins { id("sky.spring-service-conventions") }` + JPA/data-rest/validation/web/webflux/mysql/gson/kafka/springdoc/jaxb/mockwebserver + spring-kafka-test + spring-test. Per-service wrapper/settings deleted.
- [x] 3.2 sky-offer: same shape, no mockwebserver.
- [x] 3.3 sky-message: JPA + web only (no kafka, no webflux).
- [x] 3.4 sky-notify: spring-boot-starter + websocket + kafka + the existing `compileOnly extendsFrom annotationProcessor` rule preserved for Lombok+config-processor cooperation.
- [x] 3.5 `config/microservicesConfig.gradle.kts` deleted.

## 4. Update external references

- [ ] 4.1 Dockerfiles still build from each service's dir. Deferred to `docker-modernization`.
- [ ] 4.2 K8s deploy scripts don't invoke gradle directly (they `kubectl apply`). Nothing to update.
- [ ] 4.3 Root README build instructions — deferred to a later doc-sweep change.

## 5. Verify

- [x] 5.1 From repo root: `./gradlew help` — root + every subproject configures cleanly. Convention plugin compiles and `libs` resolves inside it.
- [ ] 5.2 `./gradlew test` — deferred; existing tests depend on the live MySQL defaults the `spring-boot-cleanup` change removes. Verify after that.
- [ ] 5.3 `./gradlew :sky-booking:bootJar` — deferred to a later integration check.
- [x] 5.4 `find . -name gradlew` returns only `./gradlew` (per-service wrappers removed).
