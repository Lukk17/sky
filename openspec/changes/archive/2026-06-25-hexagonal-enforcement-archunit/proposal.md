## Why

The four services already follow a hexagonal layout (`adapters/`, `domain/`, `config/`) — but only by convention. There is no test or compile-time check stopping a controller from injecting a repository directly, an entity from leaking through a controller method signature, or a primary adapter from depending on a secondary adapter. ArchUnit converts the convention into enforced contracts so the architecture survives future contributors. The change also normalizes the small inconsistencies (notify has `inbound/`/`outbound/` under `adapters/` while others don't; one service exposes a controller without `@RequestMapping`).

## What Changes

- **Adopt** ArchUnit (`com.tngtech.archunit:archunit-junit5`) as a test dependency via `sky-common` (test-fixtures classifier).
- **Add** `sky-common/src/testFixtures/java/com/lukk/sky/common/archunit/HexagonalRules.java` with reusable rule constants.
- **Add** per-service `<Service>ArchitectureTest` class running:
  - **Layer rules**: `adapters` may depend on `domain` and `sky.common`; `domain` may depend on nothing outside itself and `sky.common`; `config` may depend on both.
  - **Naming rules**: classes in `domain.ports` end with `Port`; classes implementing ports end with `Primary` (current convention) or `Adapter`; entities in `domain.model`; controllers in `adapters.api` end with `Controller`; Kafka consumers in `adapters.inbound`; HTTP/WebSocket emitters in `adapters.outbound`.
  - **Cross-cutting rules**: no JPA `@Entity` types appear in controller method signatures (DTO boundary); no Spring `@Service` in `domain.*`; no `RestTemplate`/`WebClient` in `domain.*`.
- **Normalize** package layouts so all services have the same shape:
  - `adapters.api` (REST controllers + DTOs)
  - `adapters.inbound` (Kafka listeners, WebSocket inbound)
  - `adapters.outbound` (HTTP clients, Kafka producers, WebSocket emitters, notification publishers)
  - `adapters.persistence` (JPA repositories + entity adapter mappers if any)
  - `domain.model` (entities + value objects)
  - `domain.ports` (interfaces — primary = use-case ports, secondary = SPI ports)
  - `domain.service` (use-case implementations, `*ServicePrimary` keeps current name)
  - `domain.exception`
  - `config`
- **Move** files in each service to match. Booking, offer, message: minor moves. Notify: already close.
- **Add** `@RequestMapping("/api/internal")` to sky-offer's `OfferInternalController` (currently routes from root).

## Capabilities

### New Capabilities
- `hexagonal-enforcement`: ArchUnit rules in `sky-common` test fixtures, applied per service; standardized package layout enforced; primary-vs-secondary port distinction explicit.

### Modified Capabilities
- _None._ No external behavior; internal package moves only.

## Impact

- **Touched files**: every service has ~10–20 files moved to the new package structure; every service gets one new ArchUnit test class. `sky-common` gains test-fixtures.
- **Import statements**: every internal import touching a moved class updates. IDE-driven refactor; mechanical.
- **CI**: ArchUnit tests run as part of normal `./gradlew test`. Any future PR violating the rules fails fast.
- **OfferInternalController route change**: `GET /owner/offer/{id}` → `GET /api/internal/owner/offer/{id}`. Only sky-booking's `RestClientWebflux` calls this — update there too. No external clients exist (per audit).
- **Risk**: medium. Lots of files move, but no logic changes. Tests verify nothing regressed.
- **Dependency order**: depends on `extract-sky-common` (for test fixtures) and `gradle-multi-project`. Should land before any change that adds new code (otherwise that code starts out misplaced).
