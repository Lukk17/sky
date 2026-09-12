# AGENTS.md: sky-common

Module-local guidance for `sky-common`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-common` is a shared library, not a deployable service. It has no `main` class, no `bootJar`, and no port. All
five other modules (`sky-booking`, `sky-offer`, `sky-message`, `sky-notify` and `sky-gateway`) depend on it via
`implementation(project(":sky-common"))` to share wire types, Spring auto-configurations, security defaults, and web
utilities.

Current `description = "sky-common: shared wire types, auto-configurations, and web utilities"`. The module version is
not restated here: the topmost `## [x.y.z]` entry in [CHANGELOG.md](CHANGELOG.md) is the current one, and it wins over
the cosmetic `version` in `build.gradle.kts`.

## Architecture

- Build plugin: `sky.java-library-conventions` (a plain Java library: no Spring Boot plugin and no `bootJar`), in
  contrast to the `sky.spring-service-conventions` every other module uses. The coverage gate is not one of the
  differences: `sky.java-library-conventions` applies `sky.java-conventions`, which applies `sky.jacoco-conventions`,
  so this module carries the same 0.90 line and 0.90 branch `jacocoTestCoverageVerification` wired into `check` as the
  four services do. `sky-gateway` depends on it too, so this library now has five consumers, not four.
- Ten registered auto-configurations, in the order
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` lists them:
  `CorrelationIdAutoConfiguration`, `ApiVersioningAutoConfiguration`, `RestExceptionHandlerAutoConfiguration`,
  `ResourceServerJwtAutoConfiguration`, `LocalSecurityAutoConfiguration`, `MethodSecurityAutoConfiguration`,
  `OpenApiAutoConfiguration`, `OpenApiSecurityAutoConfiguration`, `CommonConfigPropertiesAutoConfiguration`,
  `StartupLogConfig`. That file is the contract: a class not listed there never runs in a consumer, whatever
  annotations it carries.
- Package layout (`com.lukk.sky.common`), six packages:
  - `common.security`: the largest package. `SecurityPaths` (the shared permit-lists: `probes()` is
    `/actuator/health/**` plus `/actuator/info`, `apiDocs()` is the springdoc paths, plus `probesAndApiDocs()` and
    `probesPlus(...)`), `SkySecurityDefaults.filterChain(...)` (the one servlet chain every service builds on: CSRF
    off, permit-list open, JWT resource server on, everything else authenticated), `ResourceServerJwtAutoConfiguration`
    (the role converter, plus `AudienceValidator` when `OAUTH2_AUDIENCE` is set, and deliberately no `JwtDecoder`
    bean, because Spring Boot builds that from `spring.security.oauth2.resourceserver.jwt.issuer-uri`, which each
    service fills from `OAUTH2_ISSUER_URI`. A test asserts the absence, so do not add one),
    `LocalSecurityAutoConfiguration` with `UnverifiedJwtDecoder` (`@Profile("local")` only, accepts any well-formed
    token so a local run needs no reachable IdP, and logs a warning saying so), `KeycloakRealmRoleConverter`,
    `MethodSecurityAutoConfiguration` with the `@IsUser` meta-annotation, and `SecurityUtils.currentUserEmail()`.
  - `common.web`: `SkyRestExceptionHandler` and `RestExceptionHandlerAutoConfiguration` (RFC 9457 problem details for
    every service. There is no exception-handler base class to extend, the handler is auto-configured),
    `CorrelationId`, `CorrelationIdFilter`, `CorrelationIdClientHttpRequestInterceptor` and
    `CorrelationIdAutoConfiguration` (read or mint `X-Correlation-Id`, put it in the MDC, pass it outbound),
    `ApiVersioningAutoConfiguration`, and `DateTimeConstants`.
  - `common.kafka`: `KafkaPayloadModel` (the wire envelope), `KafkaNotificationPublisher` (the shared producer,
    serialising through the injected Jackson 3 `ObjectMapper`), and `SkyTopics`.
  - `common.openapi`: `OpenApiAutoConfiguration`, `OpenApiSecurityAutoConfiguration`, and two response annotations:
    `@ApiCommonErrorResponses` (400 and 500) and `@ApiSecuredErrorResponses` (401 and 403, for an endpoint that needs
    a token). There is no `@ApiCommonSuccessResponses`.
  - `common.config`: `CommonConfigPropertiesAutoConfiguration` binding the shared server, management and
    logging-level property records.
  - `common.startup`: `StartupLogConfig`, which emits the whole startup readiness block in one `log.info` call.
- Dependency discipline: Spring Web, Spring MVC, Jakarta Servlet, Jakarta Validation, SLF4J, JSpecify, Spring
  Kafka, Jackson Databind, springdoc, `spring-boot-autoconfigure` and `spring-boot-starter-oauth2-resource-server` are
  all declared `compileOnly` on purpose. A consumer that is not a web service (or not a Kafka service, or has no
  Swagger UI) must not pull Spring MVC, `spring-kafka` or springdoc transitively just by depending on `sky-common`.
  Tests re-add the real dependencies via `testImplementation`.
- The one Gson left in the repository is `testImplementation(libs.gson)` here, used purely as the oracle in
  `KafkaNotificationPublisherTest` to prove the Jackson output is byte-identical to what the old Gson code produced.
  Every `main` source set across every module is Gson-free. Drop the test dependency and the catalogue alias once that
  wire-parity check is no longer worth keeping.

## Conventions

- When adding a shared type, keep the `compileOnly` rule: if the new code needs a runtime dependency, declare it
  `compileOnly` here and let each consuming service opt in with its own `implementation` entry. Do not promote a
  `compileOnly` dependency to `implementation` without checking every consumer.
- Auto-configurations are registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  A new auto-config class must be added there to take effect in consumers.
- This is a library: there is no `application.yaml` and nothing to run standalone. It does have its own test suite
  (`ApplicationContextRunner`-style slices for the auto-configurations, plain unit tests for the rest), so verify a
  change here and in at least one consuming service's tests, because the `compileOnly` split means a class can
  compile here and still be missing a runtime dependency downstream.
- `LocalSecurityAutoConfiguration` is the one profile-sensitive class in this module: it is `@Profile("local")` and it
  disables JWT signature, issuer and expiry checks. Never widen that profile condition, and never make the unverified
  decoder the fallback for a missing issuer.
