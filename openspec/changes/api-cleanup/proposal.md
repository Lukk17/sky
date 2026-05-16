## Why

Three small but durable cleanups to the REST API:

1. **No versioning today.** `/api/...` everywhere, no version segment, no version header. The moment we need a breaking change to a payload, we'll regret this. Spring Framework 7 (shipping with Spring Boot 4) introduced native API versioning — pluggable resolvers (header, path, media-type, query) and `@RequestMapping(version=...)`. Adopt the native mechanism rather than rolling URL prefixes.
2. **Resource-naming inconsistency** between services: `/api/offers` (plural), `/api/owner/offer` (singular), `/api/messages/received` (plural with sub-path), `/api/message/{id}` (singular). Standardize on REST plural-noun convention.
3. **`OfferInternalController` route bug** — class has no `@RequestMapping`, so its endpoint lives at root: `GET /owner/offer/{id}`. Fixed in `hexagonal-enforcement-archunit` to `/api/internal/owner/offer/{id}` — but this change is where the consumer (sky-booking's REST client) and any external doc gets updated to v1.

## What Changes

- **Configure** API versioning in each service's `WebMvcConfigurer` via `configureApiVersioning(ApiVersionConfigurer)`:
  - Use `useRequestHeader("X-API-Version")` as the primary resolver. Optionally fall back to media-type (`accept: application/vnd.sky.v1+json`).
  - Set default version `"1"`.
  - Set supported versions `"1"`.
- **Annotate** every controller method (or class) with `@RequestMapping(..., version = "1")`. Initially every endpoint is v1.
- **Standardize** paths to plural nouns:
  - sky-offer: `/api/owner/offer` → `/api/owner/offers` (singular create endpoint becomes POST to plural).
  - sky-message: `/api/message` → `/api/messages` for the singular endpoints (POST + DELETE align with the existing `/api/messages/received|sent`).
- **Update** sky-booking's `OfferRestClient` (renamed in `spring-boot-cleanup`) to call `/api/internal/owner/offers/{id}` once sky-offer's internal route consolidates under v1.
- **Document** API versioning convention in root `AGENTS.md` ## Architecture (likely 2 lines: header name, supported versions).
- **Update** Postman collection paths.
- **Update** Swagger UI auto-generated docs — versioning shows up in springdoc once configured.

## Capabilities

### New Capabilities
- `api-versioning`: Header-based API versioning via `X-API-Version`, default and supported versions configured per service, every endpoint explicitly tagged with its version.

### Modified Capabilities
- _None at spec level._ Endpoint paths shift (singular → plural), tracked under the new capability.

## Impact

- **Touched files**: every controller in booking, offer, message gets a version annotation; some controllers get path edits for plural-noun consistency. New `ApiVersionConfig` per service (or shared via `sky-common`). Postman collection JSON updated. sky-booking's REST client updated.
- **External client breaking change**: sky-view (frontend) and Postman collection. Coordinate with sky-view if it pins old paths. Postman is in-repo; updated in the same commit.
- **Backwards compatibility**: optional. Spring Framework 7 versioning allows multiple `@RequestMapping(version = "1")` and `(version = "2")` on different methods, so a future v2 can ship alongside v1 with no code duplication. Out of scope here — there's only v1 today.
- **Risk**: low-medium. Path renames are mechanical; missing one breaks a client. Postman E2E catches most.
- **Dependency order**: depends on `spring-boot-4-migration` (Spring Framework 7 ships there). Also coordinates with `hexagonal-enforcement-archunit` (which already moved the internal controller's route prefix). Should be the last change in the sequence to give the cleanest "where we landed" snapshot.
