# api-versioning Specification

## Purpose
TBD - created by archiving change api-cleanup. Update Purpose after archive.
## Requirements
### Requirement: Header-based API versioning via Spring Framework 7 native mechanism
Every REST service (sky-booking, sky-offer, sky-message) MUST configure API versioning through `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` using the `X-API-Version` request header as the primary resolver. Every controller method or class MUST declare its version via `@RequestMapping(version = "1")` (or a method-level override).

#### Scenario: Default version applies when header is absent
- **WHEN** a client calls `GET /api/offers` without the `X-API-Version` header
- **THEN** the request resolves to v1 (the configured default version); the response matches the v1 contract

#### Scenario: Unsupported version is rejected
- **WHEN** a client sends `GET /api/offers` with header `X-API-Version: 99`
- **THEN** the server returns a 4xx response (per Spring Framework 7's `NotAcceptableApiVersionException` default handling)

### Requirement: REST resource paths use plural nouns consistently
Every collection endpoint MUST use a plural-noun path segment (`/offers`, `/messages`, `/bookings`). Singular paths (`/api/owner/offer`, `/api/message`) MUST be renamed to their plural equivalents.

#### Scenario: Auditing REST paths
- **WHEN** an operator inspects the Swagger UI for any service
- **THEN** every collection-style endpoint shows a plural noun in its path; no singular collection paths remain

### Requirement: Internal endpoints are versioned and namespaced
Service-to-service REST endpoints (e.g., sky-booking's lookup against sky-offer) MUST live under `/api/internal/...` and carry the same versioning as public endpoints.

#### Scenario: sky-booking calls sky-offer for offer ownership
- **WHEN** sky-booking's REST client calls the offer service to verify ownership
- **THEN** the request targets `/api/internal/owner/offers/{id}` with `X-API-Version: 1`; the route is not exposed at the root path

