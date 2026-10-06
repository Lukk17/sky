## Context

Major framework bumps are rarely "interesting" — they're a series of small adjustments. The technique is to make the bump landable by preparing the ground first (this is why this is change 15/16). By the time we bump SB, the catalog is one line, the conventions are one plugin, the duplicated code lives in `sky-common`, and the failing-most-loudly defaults (`@MockBean`, WebFlux duplication) have been removed.

Spring Boot 4 is the November 2025 GA. The notable user-facing features for Sky:
- **Native API versioning** in Spring Framework 7's MVC: `@RequestMapping(version=...)` with pluggable `ApiVersionResolver` (header, path, media-type, query). Replaces ad-hoc `/api/v1/` URL prefixing. Used in the next change (`api-cleanup`).
- **`@MockitoBean`** is the new standard test annotation; `@MockBean` is removed in SB4 (was deprecated in 3.4).
- **`RestClient`** matures further; we already adopted in `spring-boot-cleanup`.
- **Hibernate 7** bumps Jakarta Persistence API and brings native MySQL JSON column support — not exploited here but available.
- **Spring Security 7** keeps the OAuth2 resource-server API stable; minor builder-method shifts.

Java 25 is required. The repo currently uses Java 21; the bump is non-trivial only because of CI / Docker base image coordination, not because of language changes.

## Goals / Non-Goals

**Goals:**
- All services run on Spring Boot 4 / Java 25.
- All tests pass.
- Deprecated test annotations replaced (`@MockBean` → `@MockitoBean`).
- No regression in REST, Kafka, WebSocket flows.

**Non-Goals:**
- Adopting every new SB4 feature (Hibernate 7 JSON columns, Spring AI, etc.). Bump only.
- Migrating `TestRestTemplate` → `RestClient` in tests. Still supported; separate change if desired.
- Jakarta migration. Already done in SB3.
- Reactive vs servlet rethink. Already resolved in `spring-boot-cleanup`.

## Decisions

1. **Bump JDK 21 → 25 in one step.** SB4 doesn't accept JDK 21. No interim 24 step needed.
2. **Catalog-only version edits.** Because of `gradle-version-catalog` upstream, the SB version lives in one TOML line. Bumping is a 1-line PR for the core; the rest is migrations.
3. **Defer optional bumps**: don't chase the absolute latest of every transitive lib in this PR — let Spring Boot BOM resolve them. Avoids "while I'm in here" creep.
4. **Coordinate Docker base bump with `docker-modernization`.** That change sets the runtime image to `eclipse-temurin:21-jre-alpine`; this change re-bumps to `:25-jre-alpine` (or whatever the equivalent tag is at apply time). One commit per image change so bisect remains clean.
5. **`@MockitoBean` is the only mandatory annotation rename** — no other deprecations are in the codebase per audit.
6. **Spring Security 7 surface area** in sky-notify is `SecurityFilterChain` + `oauth2-resource-server` config; the API has been stable for two majors. Expected minor adjustment only.

## Risks / Trade-offs

- **Transitive lib breakage**: any third-party that bundles Spring Boot 3 classes will conflict. We have one — `springdoc-openapi`. The springdoc 3.x line targets SB4; ensure the catalog picks the right version.
- **Java 25 features tempt scope creep.** Resist. Adopt later in feature work.
- **JaCoCo + Java 25**: JaCoCo lags Java releases by a few weeks. Verify the version pinned in the catalog supports class-file version 69 (Java 25). If not, pin a snapshot or temporarily lower thresholds.
- **Testcontainers + JDK 25**: Testcontainers itself runs as a client; the test JVM is on JDK 25; the MySQL/Kafka containers are unaffected. Should work without changes.
- **Build cache invalidation**: bumping JDK toolchain invalidates Gradle cache. First build after merge is slow; acceptable.
- **Rolling deploy**: services on SB4 + SB3 versions co-existing in cluster is fine for our case (no shared classpath); deploy in any order.
