## 1. ArchUnit infrastructure

- [x] 1.1 Add `archunit-junit5` to the catalog under `[libraries]`.
- [x] 1.2 Add `sky-common/build.gradle.kts` `java { withSourcesJar(); registerFeature(...) }` or simply expose a `testFixtures` source set with ArchUnit dep.
- [x] 1.3 Create `sky-common/src/testFixtures/java/com/lukk/sky/common/archunit/SkyArchitectures.java` with the layered architecture definition: `adapters` → `domain` → `sky.common`; `config` may touch both; `domain` is allowed to import `sky.common` and `java.*` only.
- [x] 1.4 Add `SkyNamingRules.java` for the naming conventions (Port suffix, Controller suffix, etc.).
- [x] 1.5 Add `SkyCrossCuttingRules.java`: no `@Entity` in controller signatures, no `WebClient`/`RestTemplate` in `domain.*`, no `org.springframework.stereotype.Service` annotation in `domain.*` (use plain classes; injection happens in `domain.service` which is allowed).

## 2. Normalize package layout per service

- [x] 2.1 sky-booking: move `adapters/api` controllers + DTOs as-is, move `adapters/notification` → `adapters/outbound`, repositories to `adapters/persistence`, services to `domain/service`. Verify all imports compile.
- [x] 2.2 sky-offer: same. Move `OfferInternalController` and add `@RequestMapping("/api/internal")`.
- [x] 2.3 sky-message: same.
- [x] 2.4 sky-notify: rename existing `inbound`/`outbound` already under `adapters/` to confirm. Move `WebSocketConfig` from `config` if it has logic (or keep — config is allowed).

## 3. Wire ArchUnit tests per service

- [x] 3.1 Add `testImplementation(testFixtures(project(":sky-common")))` to each service.
- [x] 3.2 Create `sky-booking/src/test/java/com/lukk/sky/booking/architecture/BookingArchitectureTest.java` running `SkyArchitectures.HEXAGONAL.check(classes)` + naming + cross-cutting rules.
- [x] 3.3 Repeat per service.
- [x] 3.4 Use `JavaClasses` import limited to the service's base package so cross-service imports (which shouldn't exist) trigger separate rule violations.

## 4. Fix the internal-controller path coupling

- [x] 4.1 Update sky-booking's `RestClientWebflux` (in `adapters/outbound`) to call `/api/internal/owner/offer/{id}` instead of `/owner/offer/{id}`.
- [x] 4.2 Update sky-booking's integration test mockwebserver setup to expect the new path.

## 5. Verify

- [x] 5.1 `./gradlew test` across all services — all ArchUnit + existing tests pass.
- [x] 5.2 Intentionally break a rule (e.g., have a controller depend on a repository directly); confirm ArchUnit fails the test.
- [x] 5.3 sky-booking integration test against sky-offer's new internal path works end to end.
- [x] 5.4 Update root `AGENTS.md` `## Architecture` section to mention ArchUnit and the package layout contract.
