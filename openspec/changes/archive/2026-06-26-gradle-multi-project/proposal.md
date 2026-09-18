## Why

The four services are siblings in one repo but each has its own `gradlew`, `gradle/wrapper/`, `settings.gradle.kts`, and 85–90% duplicated `build.gradle.kts`. There is no root build, no shared convention plugin, and no way to run `./gradlew build` once. Bumping a wrapper version means editing four `gradle-wrapper.properties` files. Adding a new common dependency means editing four build files. A Gradle multi-project with a `buildSrc/` convention plugin fixes all of this and is a precondition for `extract-sky-common`.

## What Changes

- **Add** root `settings.gradle.kts` that includes `:sky-booking`, `:sky-offer`, `:sky-message`, `:sky-notify` (and later `:sky-common`).
- **Add** root `build.gradle.kts` (minimal — plugin management, group, version).
- **Add** root `gradlew`, `gradlew.bat`, `gradle/wrapper/` directory with single source of truth.
- **Delete** per-service `gradlew`, `gradlew.bat`, `gradle/wrapper/` (4 × 3 files = 12 deletions).
- **Delete** per-service `settings.gradle.kts` (replaced by root).
- **Add** `buildSrc/` directory with convention plugins:
  - `sky.java-conventions.gradle.kts` — Java toolchain, sourceCompatibility, repositories, common test config.
  - `sky.spring-service-conventions.gradle.kts` — Spring Boot plugin, dependency-management, actuator, configuration-processor, lombok, jaxb, security-test, h2, junit, profiles.
  - `sky.kafka-conventions.gradle.kts` — spring-kafka + spring-kafka-test.
  - `sky.web-conventions.gradle.kts` — spring-boot-starter-web, validation, springdoc-webmvc-ui.
- **Migrate** each service `build.gradle.kts` to `plugins { id("sky.spring-service-conventions"); ... }` + only its service-specific deps.
- **Move** `config/microservicesConfig.gradle.kts` content (group, version refs) into root `build.gradle.kts` or convention plugins. Delete the file.

## Capabilities

### New Capabilities
- `gradle-multi-project`: Single rooted Gradle build, single wrapper, single source of truth for shared service configuration via convention plugins under `buildSrc/`.

### Modified Capabilities
- _None._ Pure structural refactor; service deps remain identical.

## Impact

- **Touched files**: ~30 files changed/deleted/created in `config/`, root, each service's `gradle*`, each service's `build.gradle.kts`, each service's `settings.gradle.kts`.
- **CI/CD**: deploy scripts under `config/k8s/_deployment-scripts/` may invoke per-service gradle wrappers. Update to use root `./gradlew :sky-booking:bootJar`.
- **IDE**: IntelliJ users re-import once.
- **Docker**: per-service `docker/Dockerfile` likely runs `./gradlew build` against the service directory; will need to use `./gradlew :sky-booking:bootJar` from repo root, OR keep the service-folder context and switch to a root-aware build. Coordinated with `docker-modernization`.
- **Risk**: medium. Build-graph reshape, but no behavior changes. Loud failures at Gradle configure time if wrong; easy to validate.
- **Dependency order**: must land before `extract-sky-common` (the new module needs the multi-project setup). Best to land after `gradle-version-catalog` so the convention plugins can reference `libs.*` directly.
