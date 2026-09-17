# Changelog: sky-common

All notable changes to this module are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to
[Semantic Versioning](https://semver.org/). Newest entry on top. The topmost `## [x.y.z]`
version is the current one. This module is a library with no image of its own: it is
compiled into all five service images, so a change here needs a version bump in this file
and in every service that ships it. The `version` in `build.gradle.kts` is a cosmetic
label the release workflow does not read. If the two disagree, this file wins.

## [2.0.0]

### Added

- A last-resort 500, so an exception nobody mapped stops answering in a second shape.
  `UnhandledExceptionResolver` answers `application/problem+json` with `title` `Internal Server Error`,
  `instance` set to the request URI, and a fixed `detail` that names no exception type, no message, no
  class and no package, replacing Spring Boot's flat `{timestamp, status, error, path}` body on
  `application/json`. It logs `unhandled_exception` at error with the full stack, the method and the
  path, because resolving the exception stops Tomcat logging it and this becomes the only record of the
  failure. The correlation id reaches the line through the `[%X{correlationId}]` each service already
  has in its logback pattern, which is why the message does not repeat it. It is a `HandlerExceptionResolver` at `Ordered.LOWEST_PRECEDENCE` rather
  than a `@ControllerAdvice`: `ExceptionHandlerExceptionResolver` returns from the first advice that
  matches at all, the three service `GlobalExceptionHandler` classes declare no order and therefore sit
  at `Ordered.LOWEST_PRECEDENCE` already, and no advice can sort behind them, so a catch-all advice
  would have turned every 503, 502, 409 and 404 into a 500. A top-level resolver runs after the whole
  `HandlerExceptionResolverComposite`, which holds every advice, and it also catches the
  `DataIntegrityViolationException` that `SpringDataExceptionHandler` rethrows when it declines, which
  no advice can see. It is a bean on `RestExceptionHandlerAutoConfiguration` and inherits that class's
  `@ConditionalOnWebApplication(SERVLET)` and `@ConditionalOnClass(ResponseEntityExceptionHandler.class)`
  gates unchanged, so it never loads in sky-gateway, where spring-webmvc is absent, and is inert in
  sky-notify, which maps no request.

- A startup credential check, so a missing environment variable fails the boot loudly instead of
  surfacing much later as a connection error that names nothing. Spring Boot's
  `PropertySourcesPlaceholdersResolver` builds its placeholder helper with unresolvable placeholders
  ignored, hardcoded, so `password: ${POSTGRES_PASSWORD}` with the variable unset binds as that
  literal text rather than failing. `DatasourceCredentialsAutoConfiguration` registers a
  `BeanPostProcessor` that reads the effective `JdbcConnectionDetails` and refuses to continue when
  the username or the password is still a bare placeholder. It is gated on
  `@ConditionalOnClass(JdbcConnectionDetails.class)`, which `spring-boot-jdbc` supplies and which
  neither sky-notify nor sky-gateway carries, so it never loads in a module with no datasource. It
  reads the connection details rather than the raw property because Testcontainers supplies the real
  credentials through that same interface, so an integration test with no environment variable stays
  silent. It hooks `postProcessAfterInitialization` rather than the earlier callback, because the
  Testcontainers implementation refuses to hand out a credential before its own initialization has
  run, and it still beats Flyway and every connection attempt because the `DataSource` depends on the
  bean being checked.
- `RequiredCredentials`, the entry point any service can use for a credential of its own, and
  `MissingCredentialException`, which names every missing variable at once rather than one per
  restart, names the property each one feeds, quotes the literal text the reader is looking at, and
  explains the framework behaviour behind it. `MissingCredentialFailureAnalyzer`, registered in a new
  `META-INF/spring.factories`, renders that as Spring Boot's `APPLICATION FAILED TO START` block with
  a Description and an Action instead of a stack trace. The `local` profile is unaffected: its
  configuration files default both database credentials, so the check finds a value and says nothing.
- `spring-boot-jdbc` as a `compileOnly` dependency, for `JdbcConnectionDetails`. It is already on the
  runtime classpath of the three stateful services through the JPA starter, and the `compileOnly`
  declaration keeps it off sky-notify and sky-gateway exactly as the rest of this module's
  dependencies are kept off consumers that do not need them.
- Four more OpenAPI response annotations in `common.openapi`, so a status that two services both answer is
  described once rather than copied into each controller. `@ApiConflictResponse` carries the 409 both
  sky-booking and sky-offer raise when a well-formed write loses a race, `@ApiUnsupportedMediaTypeResponse`
  the 415 every operation with a request body can answer, `@ApiDependencyUnavailableResponse` the 503
  sky-booking answers for an unreachable sky-offer and sky-offer answers for an unusable object store, and
  `@ApiDependencyBadGatewayResponse` the 502 each of them answers when that dependency replies in a way it
  cannot act on. Their wording names no particular dependency on purpose, because one annotation has to read
  correctly above an offer lookup and above an object store alike. They join `@ApiCommonErrorResponses` and
  `@ApiSecuredErrorResponses`, and they are registered nowhere: a plain annotation is not an
  auto-configuration, so
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` is unchanged. The 503
  names `Retry-After` and its fixed value of 10 in its description rather than declaring the header, because
  the response headers across this contract are a separate piece of work nobody has taken yet.
- This module is new in 2.0.0. Before it, each of the four services carried its own copy of
  the same wire types, property bindings, exception handling and security wiring, and the
  copies had already drifted: three different `KafkaPayloadModel` records, three different
  `SwaggerConfig` classes, and four `ServerConfigProperties` that bound the same keys with
  different defaults. There is one copy of each of those now, and the drift is gone.
- Spring Boot auto-configuration is the delivery mechanism rather than component scanning,
  registered through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  Ten auto-configurations ship: correlation id, API versioning, the REST exception handler,
  resource-server JWT, local-profile security, method security, two OpenAPI configurations,
  common config properties, and the startup log. A service gets them by depending on this
  module, with no annotation to remember, and each one is guarded so a service that lacks
  the underlying library simply does not get the bean.
- `SkyRestExceptionHandler` turns every framework-level web exception into an RFC 9457
  `ProblemDetail` response, and a Bean Validation failure additionally carries a
  `field-errors` object mapping each rejected field to its message. Callers that parsed the
  old free-text error strings have to read the problem document instead.
- `ResourceServerJwtAutoConfiguration` with `KeycloakRealmRoleConverter` and
  `AudienceValidator` gives every service the same JWT contract: the realm roles in the
  token become Spring authorities, and a token whose `aud` claim does not name this backend
  is rejected rather than accepted on signature alone.
- `LocalSecurityAutoConfiguration` installs an `UnverifiedJwtDecoder` under the `local`
  profile only, so a developer can drive the stack with a hand-made token and no running
  identity provider. It logs
  `security.jwt_validation_disabled profile=local anyWellFormedTokenAccepted=true` at WARN
  every startup, because a silent bypass is how one ends up in production.
- `ApiVersioningAutoConfiguration` configures Spring Framework 7 native API versioning off
  path segment 1, so `/api/v1/...` is parsed as version `1` by the framework rather than
  being a string the controllers happen to share. Version `1` is both the default and the
  only supported version, which means an unknown version is answered by the framework
  rather than reaching a handler.
- `CorrelationIdFilter` and `CorrelationIdClientHttpRequestInterceptor` put a correlation id
  on every inbound request and carry it onto every outbound one, so a booking that fans out
  to sky-offer and then to Kafka can be followed across three services in the logs.
- `SecurityUtils.currentUserEmail()` reads the caller identity from the validated token.
  Every service previously dug the identity out of a request header map with a private
  helper, which is the part that made the old trusted-header model possible.
- `SkyTopics` names `bookingTopic-1` and `offerTopic-1` in one place, shared by the two
  producers and the one consumer, so a topic rename can no longer half land.

### Changed
- Every Spring dependency is declared `compileOnly` on purpose, and tests re-add the real
  ones with `testImplementation`. A consumer that is not a web service must not get Spring
  MVC transitively just by depending on this module, and sky-notify in particular must not
  get Spring Data or springdoc. Promoting one of these to `implementation` changes the
  classpath of all five services, so check every consumer before doing it.
- `StartupLogConfig` is now `@ConditionalOnClass({RestClient.class, SimpleClientHttpRequestFactory.class})`,
  the two Spring Web types its JWK-set probe builds. Spring Web is `compileOnly` here, so the class
  reached a consumer that could not run that method, and the `catch (Exception e)` around the probe does
  not catch the `NoClassDefFoundError` that would follow. Every consumer carries Spring Web today,
  sky-gateway through WebFlux, so no module loses the startup log. The condition is not
  `@ConditionalOnWebApplication`, which would either gate nothing or take the log away from the gateway.

### Fixed
- `@ApiCommonErrorResponses` documents the 500 as the problem detail it now is, with the
  `application/problem+json` media type and the `ProblemDetail` schema the 400 already carried. It
  still told every reader of every service's Swagger UI that a 500 is Spring Boot's default error
  object and not a problem detail, which stopped being true when `UnhandledExceptionResolver`
  landed, so the live documentation contradicted the behaviour on all four services.
- Event timestamps are written as `timestamptz` rather than a naive local type, so a
  timestamp no longer shifts meaning when the writer and the reader disagree about the
  server time zone.
