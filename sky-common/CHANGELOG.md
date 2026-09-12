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

### Fixed
- Event timestamps are written as `timestamptz` rather than a naive local type, so a
  timestamp no longer shifts meaning when the writer and the reader disagree about the
  server time zone.
