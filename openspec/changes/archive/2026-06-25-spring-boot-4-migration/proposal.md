## Why

Spring Boot 4 / Spring Framework 7 (GA November 2025) is the modernization target. Most of the value comes from associated changes (new API versioning, RestClient maturity, `@MockitoBean` standardized, Jackson 3 ready, Spring Security 7, Hibernate 7 with native MySQL JSON support). The current Spring Boot 3.5.4 baseline is fine; staying there indefinitely accumulates lag and blocks the API-versioning native support called for in `api-cleanup`.

This change is intentionally the **last** Spring code change in the sequence. By the time it lands, all the prerequisites have been ironed out:

- Convention plugin + version catalog let the SB version live in one TOML line.
- `extract-sky-common` already consolidated the duplicated configs that would otherwise need 4× the migration work.
- `kafka-reliability` already swapped to current Spring Kafka idioms (`DefaultErrorHandler`, `MANUAL_IMMEDIATE`).
- `spring-boot-cleanup` already removed the WebFlux/WebMvc mixing and `data-rest` that would compound migration toil.
- `test-modernization` already replaced `@EmbeddedKafka`/H2 with Testcontainers (which keep working in SB4).

The remaining migration work is narrow: bump Java, bump versions, swap `@MockBean` → `@MockitoBean`, audit removed deprecations.

## What Changes

- **Java**: 21 → 25 in the `[versions]` table and in convention plugin toolchain config. SB4 requires JDK 25 minimum.
- **Spring Boot**: 3.5.4 → 4.0.x (latest patch) in `libs.versions.toml`.
- **Spring dependency-management plugin**: 1.1.7 → 1.1.7 (Gradle plugin; check for compatible patch).
- **`@MockBean` → `@MockitoBean`** in all 5 test files identified by the audit:
  - sky-booking/src/test/java/.../BookingControllerTest.java (2)
  - sky-message/src/test/java/.../MessageControllerTest.java (1)
  - sky-offer/src/test/java/.../OfferApiControllerTest.java (2)
  - sky-offer/src/test/java/.../OfferInternalControllerTest.java
- **`RestClient` review**: SB4's `RestClient` is largely identical to 3.2+; the migration from sky-booking's swap in `spring-boot-cleanup` should remain compatible. Confirm in apply.
- **`TestRestTemplate`** still supported in SB4; leave for now. Tracked as a follow-up.
- **Hibernate 7**: comes with SB4. Most entities unchanged; verify any `@TypeDef` / custom UserType (none expected from audit).
- **Spring Security 7**: small API shifts; sky-notify (`websocket-auth`) JWT setup uses the modern OAuth2 resource server which carries forward.
- **Spring Kafka 4** ships with SB4; check for `DefaultErrorHandler` / `Acknowledgment` signature drift (minimal).
- **springdoc-openapi**: bump to the SB4-compatible release (3.x line). Update `libs.versions.toml`.
- **JUnit Jupiter**: SB4 ships with 5.13+; bump in catalog.
- **All other deps**: let Spring Boot 4 BOM resolve transitive bumps; verify nothing breaks.
- **Docker base images**: bump in `docker-modernization` to JDK 25 / JRE 25 (coordinated change).
- **Verify** `./gradlew dependencyUpdates` (or equivalent) shows no other catalog entry is out of supported range.

## Capabilities

### New Capabilities
- _None._

### Modified Capabilities
- _None at the spec level._ This is a framework version bump; behavior unchanged.

## Impact

- **Touched files**: `gradle/libs.versions.toml` (versions table), convention plugin (Java toolchain), 5 test files for the `@MockBean` rename, possibly a handful of import statements if Spring renamed packages (none anticipated in our scope), Docker base images.
- **CI**: build runners need JDK 25 available. GitHub Actions has `actions/setup-java@v4` with JDK 25; otherwise document the toolchain.
- **Runtime**: services pick up Spring Framework 7 / Hibernate 7 / Spring Security 7 / Spring Kafka 4. Each is a minor bump from the 3.x line; combined, the risk surface is real but well-trodden.
- **Risk**: medium. Deferred to last so it doesn't gate other improvements. Likely the most "fix small things until they all work" change of the 16.
- **Dependency order**: last. Prerequisites: `gradle-version-catalog`, `gradle-multi-project`, `extract-sky-common`, `kafka-reliability`, `spring-boot-cleanup`, `test-modernization`. `api-cleanup` follows immediately after this to use the native API versioning.
