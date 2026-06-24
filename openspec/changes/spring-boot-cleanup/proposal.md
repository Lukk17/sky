## Why

Five Spring Boot setup issues are individually small but collectively a stain on the build:

1. **`spring-boot-starter-data-rest` declared in all three data services and never used.** Adding `@RepositoryRestResource` to a repo (intentionally or by accident) would auto-expose internal JPA repositories as REST. Attack surface that does not exist if we remove the starter.
2. **Both `spring-boot-starter-web` and `spring-boot-starter-webflux` declared in sky-booking and sky-offer.** Servlet *and* reactive stacks on the same classpath; Spring Boot picks Tomcat (because `-web` wins) but the WebFlux beans linger and confuse devs. We use `WebClient` (reactive) only for the single sky-booking → sky-offer call; we can either keep WebFlux *only* in sky-booking (for `WebClient`) or replace with the new `RestClient` (servlet, blocking, fine for one call).
3. **Both `springdoc-openapi-starter-webflux-ui` and `springdoc-openapi-starter-webmvc-ui` declared together** in booking, offer, message. Only one should be on the classpath per service. The Swagger UI works by accident.
4. **Default MySQL password `Lukk1234` in `application.yaml`** as the fallback value of the env var.
5. **`crossOrigin.allowed: *`** as default CORS — wildcard in production.

These are all "fix once, never think about again" cleanups. Grouped because they share a small surface and same kind of risk (config-level only, no logic changes).

## What Changes

- **Remove** `spring-boot-starter-data-rest` from sky-booking, sky-offer, sky-message `build.gradle.kts`.
- **Decide per service** the stack:
  - sky-booking: keep `-web` (servlet), replace `WebClient` usage with `RestClient` (Spring 6.1+), remove `-webflux`.
  - sky-offer: keep `-web`, remove `-webflux`.
  - sky-message: keep `-web`, no change.
  - sky-notify: keep `-websocket` only.
- **Remove** `springdoc-openapi-starter-webflux-ui` from all three services. Keep `springdoc-openapi-starter-webmvc-ui` (matches the servlet stack).
- **Replace** `WebClient` with `RestClient` in sky-booking's `RestClientWebflux` (rename to `OfferRestClient` while at it). Functional `Mono<String>` → blocking `String` is fine here; the call is point-to-point with no fan-out.
- **Remove** default passwords from `application.yaml`. The env var becomes mandatory at startup; fail fast on missing config. Document the env vars in README (already documented; verify completeness).
- **Tighten** default CORS. In `application.yaml`: `crossOrigin.allowed: ${ACCESS_CONTROL_ALLOW_ORIGIN:https://skycloud.luksarna.com}` (named known prod origin as default; dev overrides).
- **Verify** Spring Security default user/password is fully suppressed (env-overridable as-is, but the literal `XYZ` default is a confused signal).

## Capabilities

### New Capabilities
- _None._ This is hygiene.

### Modified Capabilities
- _None._ External API and behavior unchanged. Internal `RestClient` swap is invisible to callers.

## Impact

- **Touched files**: 3 × `build.gradle.kts` (dep removals), `application.yaml` for all four services (defaults), 1 × refactor of sky-booking's REST client class + its tests (mockwebserver setup unchanged in shape).
- **Behavior risk**: `RestClient` is synchronous; the previous `WebClient` was reactive but the call site already blocked on `.block()` (verify in apply). No semantic change.
- **Startup risk**: removing default `MYSQL_PASS` means an unconfigured deployment fails on startup instead of silently using the bad default. Better to fail loud.
- **CORS risk**: stricter default could block dev frontends. Mitigation: dev-profile yaml retains the wildcard for `application-local.yaml`.
- **Risk**: low-medium. Each piece is small; coordinated to ship together to avoid bisect churn.
- **Dependency order**: independent of most other changes. Best to land after `gradle-version-catalog` so dep removals are TOML edits.
