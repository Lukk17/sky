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
- Eleven registered auto-configurations, in the order
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` lists them:
  `CorrelationIdAutoConfiguration`, `ApiVersioningAutoConfiguration`, `RestExceptionHandlerAutoConfiguration`,
  `ResourceServerJwtAutoConfiguration`, `LocalSecurityAutoConfiguration`, `MethodSecurityAutoConfiguration`,
  `OpenApiAutoConfiguration`, `OpenApiSecurityAutoConfiguration`, `CommonConfigPropertiesAutoConfiguration`,
  `DatasourceCredentialsAutoConfiguration`, `StartupLogConfig`. That file is the contract: a class not listed
  there never runs in a consumer, whatever annotations it carries. A second registration file now sits beside it,
  `META-INF/spring.factories`, holding the one `FailureAnalyzer` this module ships. A `FailureAnalyzer` is not an
  auto-configuration and Spring Boot still discovers it through `SpringFactoriesLoader`, so it belongs in that file
  and nowhere else.
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
    `UnhandledExceptionResolver` (the last-resort 500, covered in its own section below),
    `CorrelationId`, `CorrelationIdFilter`, `CorrelationIdClientHttpRequestInterceptor` and
    `CorrelationIdAutoConfiguration` (read or mint `X-Correlation-Id`, put it in the MDC, pass it outbound),
    `ApiVersioningAutoConfiguration`, and `DateTimeConstants`.
  - `common.kafka`: `KafkaPayloadModel` (the wire envelope), `KafkaNotificationPublisher` (the shared producer,
    serialising through the injected Jackson 3 `ObjectMapper`), and `SkyTopics`.
  - `common.openapi`: `OpenApiAutoConfiguration`, `OpenApiSecurityAutoConfiguration`, and two response annotations:
    `@ApiCommonErrorResponses` (400 and 500) and `@ApiSecuredErrorResponses` (401 and 403, for an endpoint that needs
    a token). There is no `@ApiCommonSuccessResponses`.
  - `common.config`: `CommonConfigPropertiesAutoConfiguration` binding the shared server, management and
    logging-level property records, plus the startup credential check covered in its own section below:
    `MissingCredential`, `MissingCredentialException`, `RequiredCredentials`, `DatasourceCredentialsValidator`,
    `DatasourceCredentialsAutoConfiguration` and `MissingCredentialFailureAnalyzer`.
  - `common.startup`: `StartupLogConfig`, which emits the whole startup readiness block in one `log.info` call. It
    is `@ConditionalOnClass({RestClient.class, SimpleClientHttpRequestFactory.class})`, both from Spring Web, which
    this module declares `compileOnly`: the JWK-set probe builds a `RestClient` on a
    `SimpleClientHttpRequestFactory`, so a consumer without Spring Web cannot run that method and must do without
    the log rather than fail on it. The condition is not `@ConditionalOnWebApplication`: `sky-gateway` is reactive
    and carries Spring Web through WebFlux, so a servlet condition would take the startup log away from a module
    that emits it today, and every consumer is a web application anyway, so an `ANY` condition would gate nothing.
- Dependency discipline: Spring Web, Spring MVC, Jakarta Servlet, Jakarta Validation, SLF4J, JSpecify, Spring
  Kafka, Jackson Databind, springdoc, `spring-boot-autoconfigure`, `spring-boot-jdbc` and
  `spring-boot-starter-oauth2-resource-server` are all declared `compileOnly` on purpose. A consumer that is not a web service (or not a Kafka service, or has no
  Swagger UI) must not pull Spring MVC, `spring-kafka` or springdoc transitively just by depending on `sky-common`.
  Tests re-add the real dependencies via `testImplementation`.
- The one Gson left in the repository is `testImplementation(libs.gson)` here, used purely as the oracle in
  `KafkaNotificationPublisherTest` to prove the Jackson output is byte-identical to what the old Gson code produced.
  Every `main` source set across every module is Gson-free. Drop the test dependency and the catalogue alias once that
  wire-parity check is no longer worth keeping.

## The last-resort 500

`UnhandledExceptionResolver` closes the one gap left after every deliberate failure in this repository became an
RFC 9457 problem detail: an exception nobody mapped fell through to Spring Boot's flat
`{timestamp, status, error, path}` body on `application/json`, so one API answered in two shapes. Read this before
moving it, because where it sits is the whole of it.

### Why it is not a `@ControllerAdvice`

A catch-all in an advice is the fastest way to undo every 503, 502, 409 and 404 in this repository.
`ExceptionHandlerExceptionResolver` walks advices in `OrderComparator` order and returns from the first one whose
resolver matches the exception at all, so a catch-all in a higher-priority advice wins over a more specific handler
in a lower-priority one. `SkyRestExceptionHandler` and `SpringDataExceptionHandler` are both `@Order(0)`, and the
three service `GlobalExceptionHandler` classes declare no order at all, which `ControllerAdviceBean.getOrder()`
resolves to `Ordered.LOWEST_PRECEDENCE`. There is no order value below that, so no second advice can sort behind
them and ties fall back to bean discovery order, which is not a contract. An advice therefore cannot be last.

### Where it sits instead

It is a `HandlerExceptionResolver` bean at `Ordered.LOWEST_PRECEDENCE`. `DispatcherServlet` detects every
`HandlerExceptionResolver` bean, sorts them, and tries each until one answers. Every advice lives inside
`ExceptionHandlerExceptionResolver`, which lives inside the `HandlerExceptionResolverComposite` that
`WebMvcConfigurationSupport` registers at order 0. So the whole advice set, at any order, is consulted before this
resolver runs. That is a structural guarantee rather than an ordering convention, and it is why the order value is
the one thing about this class that must not change.

Sitting outside the advice set buys one more case. `SpringDataExceptionHandler.handleUnstorableValue` rethrows a
`DataIntegrityViolationException` it declines, and a rethrow from an `@ExceptionHandler` makes
`ExceptionHandlerExceptionResolver` return null without trying the next advice. A catch-all advice would never see
that exception. This resolver does, because it is a separate top-level resolver.

### What it answers, and what it refuses to say

500 with `application/problem+json`, `title` `Internal Server Error`, `instance` set to `request.getRequestURI()`
the same way `RequestResponseBodyMethodProcessor` sets it for every other error, and a fixed `detail` that names no
exception type, no message, no class and no package. An unanticipated failure is exactly where an internal message
is most likely to carry something internal, which is the lesson `sky-offer` learned when an object store rejection
put its vendor message into a 400 body. It carries no `Retry-After`: nothing here knows that waiting would help.

The body is written through the converters of the application's own `RequestMappingHandlerAdapter`, taken lazily
through an `ObjectProvider`, so the response uses the same Jackson setup as every other response rather than a
second one. When no converter can write a problem detail, or the write fails, the resolver declines and the
container error page takes over, which is the behaviour that existed before this class.

### Logging

It logs `unhandled_exception` at error with the full stack, the method and the path, because resolving the
exception stops Tomcat logging it and this is now the only place the failure is visible. It puts no correlation id
in the message, exactly like the other handlers, because every service's `logback-spring.xml` already renders
`[%X{correlationId}]` in its pattern. That id is present here and would not be on the `/error` dispatch, where
`CorrelationIdFilter` has already cleared the MDC, which is a second reason the resolver runs where it does. The
same id is on the `X-Correlation-Id` response header, so the log line and the response the caller holds join on one
value.

### Where it is registered, and where it is not

It is a `@Bean` on `RestExceptionHandlerAutoConfiguration`, so it inherits that class's two gates unchanged:
`@ConditionalOnWebApplication(SERVLET)` and `@ConditionalOnClass(ResponseEntityExceptionHandler.class)`. Neither is
a module name. `sky-gateway` is reactive and carries no spring-webmvc, so the auto-configuration class never loads
there and the type cannot even link. `sky-notify` does have a servlet container, for its WebSocket handshake, so
the bean exists there exactly as `SkyRestExceptionHandler` already did, and it is inert because the module maps no
request. `UnhandledExceptionResolverInertTest` and `ServletExceptionHandlingAbsentTest` pin both halves.

Registration goes in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` only when a
new auto-configuration class is added. This is a bean on a class already listed there, so that file is unchanged,
and `META-INF/spring.factories` is for the one `FailureAnalyzer` and nothing else.

### The residual

An exception thrown inside the servlet filter chain never reaches `DispatcherServlet`, so it still lands on the
container error page with the flat body. Nothing in this repository throws there today: the security chain answers
401 and 403 by setting a status with no body, and `CorrelationIdFilter` only reads a header. Covering it would mean
replacing `BasicErrorController`, which would also change the shape of every static-resource 404 and remove the
whitelabel page. That trade was not taken.

## The startup credential check

`DatasourceCredentialsAutoConfiguration` closes a defect the merged `spring-boot-hygiene` specification names and
nothing enforced. Read this before changing anything in `common.config`, because every part of it is load bearing.

### What goes wrong without it

Spring Boot's `PropertySourcesPlaceholdersResolver` builds its `PropertyPlaceholderHelper` with
`ignoreUnresolvablePlaceholders` set to `true`. The value is hardcoded and there is no way to configure it. A
property written as `${POSTGRES_PASSWORD}` with no default and no value therefore binds as that literal text, not as
null and not as a failure. The service builds a `DataSource` around the placeholder text, and the failure surfaces
much later as a database authentication error naming neither the property nor the variable, so the person debugging
it looks at the database rather than at their environment.

### Why it lives here and not three times over

The datasource block is identical in `sky-booking`, `sky-offer` and `sky-message`. Three per-service copies would be
three copies of one rule, one message and one detection routine, and they would drift the way this repository's
`SwaggerConfig`, `Constants` and `propertyBind` copies drifted before they moved here. A per-service class also could
not be conditional on the classpath, and that condition is the part which keeps the check away from the modules that
must not have it.

### The two gates that keep it away from sky-notify and sky-gateway

Neither gate is a module name, because a module name is not a fact the container can check.

1. `@ConditionalOnClass(JdbcConnectionDetails.class)`. That type ships in `spring-boot-jdbc`, which arrives only
   with a JDBC or JPA starter. It is on the runtime classpath of the three stateful services and on neither
   `sky-notify` nor `sky-gateway`, so the auto-configuration class is never loaded there at all.
2. The validator only acts on a bean of type `JdbcConnectionDetails`. A context carrying the type on its classpath
   but holding no datasource has no such bean, so the check never runs.

`DatasourceCredentialsAutoConfigurationTest` pins both, the second through a `FilteredClassLoader` that hides
`JdbcConnectionDetails` and asserts the validator bean is absent.

### Why it reads JdbcConnectionDetails rather than the raw property

Reading `spring.datasource.password` out of the `Environment` would fire in every Testcontainers test in this
repository. Those tests leave the property at its unresolved `application.yaml` value on purpose and supply the real
credentials through a `@ServiceConnection` `JdbcConnectionDetails` bean, which overrides `DataSourceProperties`
entirely. `JdbcConnectionDetails` is the effective credential whichever route supplied it, so reading it is correct
in production and silent in a test that legitimately has no environment variable.

### Why postProcessAfterInitialization, and never before

The Testcontainers `JdbcConnectionDetails` implementation resolves its container in `afterPropertiesSet` and throws
`Container cannot be obtained before the connection details bean has been initialized` from every getter until then,
so a `postProcessBeforeInitialization` hook breaks every integration test in the three stateful services.
`postProcessAfterInitialization` still runs strictly before the `DataSource` bean is built, because the `DataSource`
depends on the connection details, so the check still beats Flyway and every connection attempt.
`DatasourceCredentialsAutoConfigurationTest` carries a late-binding stub reproducing that Testcontainers contract, so
a move back to the earlier hook fails in this module rather than four modules away.

### What counts as missing

Only a value whose whole trimmed text is a single `${NAME}` placeholder, where `NAME` holds no brace and no colon.
That is the exact shape the framework leaves behind for an unset variable, and it can never be a credential somebody
meant to configure, so the check has no false positive. Null and blank are deliberately left alone: whether an empty
password is acceptable is a separate decision this check does not make, and a check gating every boot should refuse
only what is unambiguous.

### The message

`MissingCredentialException` reports every missing credential at once rather than one per restart, names the
environment variable and the property it feeds, quotes the literal text the reader is looking at, and explains the
framework behaviour that produced it. `MissingCredentialFailureAnalyzer`, registered in `META-INF/spring.factories`,
splits that into Spring Boot's Description and Action block, so the console shows the `APPLICATION FAILED TO START`
banner with the fix inside it instead of a stack trace.

### Reusing it outside the datasource

`RequiredCredentials` is the entry point for any other credential a service binds. `sky-offer` uses it in `S3Config`
for `sky.s3.access-key` and `sky.s3.secret-key`. Prefer that over a second `BeanPostProcessor`, and do not
generalise the check into a sweep over every `@ConfigurationProperties` bean: this runs on every boot of every
deployed service, and the blast radius of a false positive is a deployment that will not start.


## Conventions

- When adding a shared type, keep the `compileOnly` rule: if the new code needs a runtime dependency, declare it
  `compileOnly` here and let each consuming service opt in with its own `implementation` entry. Do not promote a
  `compileOnly` dependency to `implementation` without checking every consumer.
- Auto-configurations are registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  A new auto-config class must be added there to take effect in consumers.
- A shared type that needs a runtime dependency still follows the `compileOnly` rule, and it must be guarded by a
  condition the container can actually check. `@ConditionalOnClass` on a type only a real consumer's classpath
  carries is the pattern `SpringDataExceptionHandler`, `DatasourceCredentialsAutoConfiguration` and
  `StartupLogConfig` all use. A condition on a module name, on a property every module happens to set, or on a
  profile is not equivalent. `StartupLogConfig` names both Spring Web types it touches rather than one standing in
  for the other, because the condition is what the reader checks the class against and a type reachable only from a
  method body is the easiest one to lose in a later edit.
- This is a library: there is no `application.yaml` and nothing to run standalone. It does have its own test suite
  (`ApplicationContextRunner`-style slices for the auto-configurations, plain unit tests for the rest), so verify a
  change here and in at least one consuming service's tests, because the `compileOnly` split means a class can
  compile here and still be missing a runtime dependency downstream.
- `LocalSecurityAutoConfiguration` is the one profile-sensitive class in this module: it is `@Profile("local")` and it
  disables JWT signature, issuer and expiry checks. Never widen that profile condition, and never make the unverified
  decoder the fallback for a missing issuer.
