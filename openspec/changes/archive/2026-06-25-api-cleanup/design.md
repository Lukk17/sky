## Context

Spring Framework 7 added first-class API versioning to the MVC and WebFlux stacks. Before 7.0, every Spring shop hand-rolled this — URL prefixes (`/v1/`), media-type negotiation, custom `RequestCondition` classes. Now `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` is the canonical entry point, and `@RequestMapping` (and its specializations) accept a `version` attribute. Resolvers are pluggable: `useRequestHeader(...)`, `useQueryParam(...)`, `usePathSegment(...)`, `useMediaTypeVersionStrategy(...)`.

For Sky, the header-based resolver is the right default: doesn't pollute URLs, doesn't require media-type negotiation, easy for the frontend to send, easy for ops to inspect in logs. Path-segment versioning (`/v1/offers`) is the second-most-common choice and is fine; we go with header for compactness.

Versioning before there's a v2 looks premature, but adding the *mechanism* now is cheap and lets the first real breaking change be a one-line decision instead of a refactor.

## Goals / Non-Goals

**Goals:**
- Every endpoint declares its version explicitly.
- Sending `X-API-Version: 1` (or no header → default v1) returns the v1 behavior.
- Sending an unsupported version returns a clear 400/406 response.
- Path naming follows plural-noun REST convention.

**Non-Goals:**
- Shipping v2 of anything. v1 only.
- Migrating the search endpoint to GET (separate discussion; semantics around complex search filters matter).
- Deep API redesign (resource hierarchies, HATEOAS, etc.). Cleanup only.
- Breaking external clients beyond the documented plural-noun renames.

## Decisions

1. **Header resolver** primary. Media-type as optional fallback. No path-segment versioning.
2. **`version = "1"` annotation on class level** (controller) where every method shares it; per-method override only when v2 is added later.
3. **Default version = "1"**, supported = `["1"]`. New v2 methods land alongside v1 ones with their own annotation; supported list expands; old v1 methods stay annotated and eventually marked deprecated.
4. **Plural-noun resource paths** — `/offers`, `/messages`, `/bookings`. Singular survives only for `/api/user` and similar "current user" semantics if any.
5. **Internal endpoints** also versioned. `/api/internal/...` is still a published contract (between sky-booking and sky-offer); same versioning rigor.
6. **`/api/home` and `/api/` hello endpoints**: drop. They're vestigial smoke checks; `/actuator/health` does the job.
7. **springdoc-openapi version**: must be SB4-compatible (3.x line). Already coordinated with `spring-boot-4-migration`.

## Risks / Trade-offs

- **Frontend (sky-view) breakage**: every renamed path breaks until sky-view ships the new paths + header. Coordinate. Mitigation: a temporary `@RequestMapping` alias on the old path during the transition deploy, removable after. Or coordinate the deploys.
- **Bruno collection regeneration**: low-tech sync issue. Done in-repo.
- **Default version behavior**: if a client forgets the header, they get v1 — fine while v1 is the only one. When v2 ships, decide whether default flips. Likely yes (newest-supported = default).
- **Hello endpoint removal**: anyone using `/api/home` as a health check should be told to use `/actuator/health/readiness`. Update Bruno + docs.
- **Coupling with `hexagonal-enforcement-archunit`**: that change moved `OfferInternalController` to `/api/internal/...`; this change adds versioning. Two consecutive edits to the same controller. Acceptable; the sequence is clear.
