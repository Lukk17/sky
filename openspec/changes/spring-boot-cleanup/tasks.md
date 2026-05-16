## 1. Remove unused data-rest

- [x] 1.1 Remove `spring-boot-starter-data-rest` from sky-booking, sky-offer, sky-message build files (or from the `sky.spring-service-conventions` convention plugin if added there).
- [x] 1.2 Grep `@RepositoryRestResource`, `@RestResource`, `org.springframework.data.rest` across the repo to confirm nothing depends on it. Expected: zero hits.
- [x] 1.3 `./gradlew build` per affected service — passes.

## 2. Resolve WebFlux/WebMvc duplication

- [x] 2.1 sky-booking: remove `spring-boot-starter-webflux`. Replace `WebClient` in `RestClientWebflux` with `RestClient`.
- [x] 2.2 sky-offer: remove `spring-boot-starter-webflux`. Verify nothing else references reactive types (Mono/Flux). If any controller returns `Mono`/`Flux`, refactor to plain return types.
- [x] 2.3 sky-booking: rename `RestClientWebflux` → `OfferRestClient`. Update the port interface name if affected.
- [x] 2.4 Update integration tests that wired `WebClient` via `WebClientTestConfig` to use `RestClient` test setup. `okhttp3 mockwebserver` continues to work; only the bean type changes.

## 3. Fix springdoc duplication

- [x] 3.1 Remove `springdoc-openapi-starter-webflux-ui` from sky-booking, sky-offer, sky-message.
- [x] 3.2 Keep `springdoc-openapi-starter-webmvc-ui`.
- [x] 3.3 Smoke: `/swagger-ui/index.html` loads on each service locally.

## 4. Defaults & secrets

- [x] 4.1 Remove default value `Lukk1234` from `spring.datasource.password`: change `${MYSQL_PASS:Lukk1234}` → `${MYSQL_PASS}` in `application.yml` for booking, offer, message.
- [x] 4.2 Same for `spring.datasource.username` if it has a default.
- [x] 4.3 Remove default `XYZ` from Spring Security user/password — better to delete the section entirely and let Spring Boot's auto-config fail open with the standard generated password warning in dev (which we ignore because we go through Ingress in prod).
- [x] 4.4 Verify each service can still start locally given `application-local.yml` supplies the values (or sets them via env in `config/local-dev/`).

## 5. CORS tightening

- [x] 5.1 Change `crossOrigin.allowed` default in `application.yml` to `https://skycloud.luksarna.com`.
- [x] 5.2 In `application-local.yml`, override to `http://localhost:4200`.
- [x] 5.3 Update CorsConfiguration consumer code to parse comma-separated origins so the env var can carry a list.

## 6. Verify

- [x] 6.1 `./gradlew build` across all services — green.
- [x] 6.2 Existing tests (including mockwebserver integration) pass.
- [x] 6.3 Smoke each service locally; Swagger UI loads; REST endpoints respond.
- [x] 6.4 `grep -rn 'WebClient\|spring-boot-starter-webflux\|data-rest\|Lukk1234' --include='*.yml' --include='*.kts' --include='*.java'` returns only intentional matches (probably none).
