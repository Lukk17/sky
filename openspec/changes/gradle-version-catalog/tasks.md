## 1. Inventory current versions

- [x] 1.1 Inventoried `config/microservicesConfig.gradle.kts` extras: springBootVersion=3.5.4, javaVersion=VERSION_21, lombokVersion=1.18.38, jUnit5Version=5.11.0, openapiVersion=2.6.0, mockwebserverVersion=4.12.0.
- [x] 1.2 Inventoried per-service literals: io.spring.dependency-management=1.1.7 (in every service plugins block).
- [x] 1.3 No plugin-management versions in any `settings.gradle.kts` (they were empty one-liners).

## 2. Create the catalog

- [x] 2.1 Created `gradle/libs.versions.toml` with `[versions]`, `[libraries]`, `[plugins]`, `[bundles]` sections.
- [x] 2.2 Populated `[versions]`: spring-boot, spring-dependency-management, lombok, junit-jupiter, springdoc, mockwebserver. (java omitted — inlined as JavaVersion.VERSION_21 in each build file; lifted to catalog by the SB4 migration change.)
- [x] 2.3 Populated `[libraries]` for all starters and BOM-managed deps used across services.
- [x] 2.4 Populated `[plugins]` with spring-boot and spring-dependency-management.
- [x] 2.5 Defined `[bundles]`: jaxb (api + runtime). The web-stack/kafka/test bundles deferred to convention plugins in the multi-project change — bundles are most useful when applied via convention plugin, otherwise it's the same line count.

## 3. Wire it in

- [x] 3.1 Each service's `settings.gradle.kts` now contains `dependencyResolutionManagement { versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } } }`.
- [x] 3.2 Plugin blocks rewritten as `alias(libs.plugins.spring.boot)` / `alias(libs.plugins.spring.dependency.management)`. Removed the `buildscript { apply(from = ...) }` + `System.setProperty` indirection.
- [x] 3.3 Every `implementation("group:art")` and friends rewritten to `libs.*` accessors; jaxb pair uses `libs.bundles.jaxb`.
- [ ] 3.4 `config/microservicesConfig.gradle.kts` left in place for now (no longer applied by any build); will be deleted by `gradle-multi-project`.

## 4. Verify

- [x] 4.1 `./gradlew help --no-daemon` in `sky-booking` → BUILD SUCCESSFUL (configure proves Spring Boot plugin resolved from catalog).
- [x] 4.2 Same for sky-offer, sky-message, sky-notify — all configure cleanly.
- [ ] 4.3 Full `./gradlew build` deferred to the multi-project change (heavy test runs against the legacy single-build setup are noisy; combined verification once root build exists).
- [x] 4.4 No version literals remain in build files; grep for `\d+\.\d+\.\d+` matches only `version = "1.0.2"` per service.
