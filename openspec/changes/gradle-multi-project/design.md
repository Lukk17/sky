## Context

Today each service is an island: its own wrapper, its own `settings.gradle.kts`, its own near-identical `build.gradle.kts`. The shared `config/microservicesConfig.gradle.kts` is loaded via `apply(from = ...)` — a 2015-era Gradle workaround that predates convention plugins by half a decade. Gradle has supported precompiled script plugins (in `buildSrc/`) since 5.0 (Nov 2018). The current setup is workable but every shared concern (toolchain version, test framework, dependency-management plugin) has to be reasserted in four places.

## Goals / Non-Goals

**Goals:**
- One wrapper, one `./gradlew`, one place to bump Gradle.
- One convention plugin per concern (java baseline, spring service, kafka, web). Service `build.gradle.kts` files become ~10 lines: which conventions + which service-specific deps.
- Repo root becomes the canonical Gradle invocation directory. CI/Docker/IDE all converge here.
- Zero behavior change for service runtime.

**Non-Goals:**
- Reorganizing the source tree (still `sky-booking/src/main/java/...`).
- Introducing Kotlin source on the service side (convention plugins are KTS; service code stays Java).
- Adding a `sky-common` module (separate change: `extract-sky-common` depends on this).
- Migrating to Maven (Gradle stays).

## Decisions

1. **`buildSrc/` not included builds.** Both are valid for convention plugins. `buildSrc/` is simpler, has zero indirection, and is the default for a single-repo project of this size. Included builds (`includeBuild`) are appropriate when convention plugins themselves need to be versioned and shared across repos — not our case.
2. **Plugin IDs use `sky.*` prefix.** Matches Gradle convention for precompiled script plugins; avoids collision with third-party IDs.
3. **One convention plugin per concern, not one per service.** Plugins compose (a service includes the conventions it needs). Per-service plugins would just re-introduce the duplication problem at a different layer.
4. **Convention plugins apply Spring Boot plugin themselves**, so each service's `plugins {}` block just declares conventions. This is the explicit `apply false / id` dance buildSrc allows.
5. **Catalog access from `buildSrc/`**: convention plugins reference the `libs` catalog via a typed accessor generated from `gradle/libs.versions.toml`. Requires `buildSrc/settings.gradle.kts` to wire up the catalog. Standard pattern.
6. **Service version (`version = "1.0.2"`)** stays in each service `build.gradle.kts` — services version independently, per release notes.

## Risks / Trade-offs

- **buildSrc invalidation cost**: any change in `buildSrc/` invalidates the build cache for all subprojects. Acceptable for a refactor-once setup; live with it.
- **IDE re-import friction**: IntelliJ users must re-import once after the change lands. Documented in README.
- **Docker build context change**: each service Dockerfile currently builds from its own dir; needs to build from repo root with `--build-context` or a path adjustment. Coupled with `docker-modernization`; sequence carefully.
- **Coupling with `gradle-version-catalog`**: this change assumes the catalog exists. If we land them in the other order, the convention plugins will start with literal versions and then refactor — extra churn. Land catalog first.
