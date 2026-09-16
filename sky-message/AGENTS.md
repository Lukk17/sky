# AGENTS.md: sky-message

Module-local guidance for `sky-message`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-message` is the messaging service: user-to-user messages. It runs on port 5553 (`MESSAGE_PORT`, default 5553),
exposes REST under the `/api/v1` prefix, and is one of the four independently-deployable services. The module version is
not restated here: the topmost `## [x.y.z]` entry in [CHANGELOG.md](CHANGELOG.md) is the current one, and it wins over
the cosmetic `version` in `build.gradle.kts`.

## Architecture

- Build plugin: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-message.jar`, JaCoCo
  report). Depends on `:sky-common`.
- Hexagonal layout (`com.lukk.sky.message`):
  - `domain/model`, `domain/exception`: core model and domain exceptions.
  - `domain/ports/inbound`: driving port interfaces called by controllers (`MessageService`).
  - `domain/ports/outbound`: driven port interfaces implemented by infrastructure adapters (`MessageRepository`).
  - `domain/service`: domain service implementations (`MessageServicePrimary`).
  - `adapters/inbound/api`: REST controllers; `adapters/dto`: wire DTOs. There is no `adapters/outbound` package:
    this service makes no outbound call of any kind.
  - `config`, `config/propertyBind`: Spring wiring and bound properties.
- MVC stack: plain Spring Web (`spring-boot-starter-web`).
- No receiver validation runs, and that is deliberate. `POST /messages` accepts any `receiverEmail` that passes Bean
  Validation and stores the message. Nothing checks the address against an identity realm or against the `message`
  table, so a message to an address nobody owns is accepted as 201 and sits in the sender's sent box unread forever.
  That is the accepted consequence of a decision the owner took after seeing what the alternative cost. The rest of
  this bullet is history, and describes a check this module no longer has. The removed check asked Keycloak's
  administration interface for a user with that email. It bought four things this module no longer carries: a 503 on
  a write path whenever Keycloak was slow, a `realm-management` `view-users` grant on a business service, a client
  secret in a service that otherwise needs none, and a user enumeration oracle one send at a time. Do not reintroduce
  it, by that route or any other, without the owner saying so.
- The only reason this service contacts Keycloak is token validation. It is an OAuth2 resource server and nothing
  more: `ResourceServerJwtAutoConfiguration` in `sky-common` fetches the signing keys from `OAUTH2_ISSUER_URI` and that
  is the whole of it. The service holds no client credential, has no `spring-security-oauth2-client` on the classpath,
  and an ArchUnit rule (`allClasses_whenInspected_thenHaveNoOAuth2ClientDependencies`) fails the build if any class
  reaches `org.springframework.security.oauth2.client..` again. The rule outlived the code it was written for on
  purpose: it is now the guard on that decision rather than a layering rule.
- No outbound HTTP, no Resilience4j. There is no `RestClient` bean, no declared `resilience4j` dependency and no
  `resilience4j` block in `application.yaml`, because the only caller of all three was the receiver lookup. A new
  outbound call means reintroducing all of it deliberately, with its own retry and circuit breaker instance, rather
  than borrowing a bean that happens to exist. `aspectjweaver` is still on the classpath, pulled in transitively by
  `spring-boot-starter-data-jpa`, so do not read its presence as an AOP dependency this module asked for.
- No Kafka: unlike `sky-offer`, `sky-booking` and `sky-notify`, `sky-message` does not depend on
  `spring-kafka`. It has no `config/kafka` package and no `adapters/outbound/notification`. Do not add Kafka here without a deliberate design decision:
  message delivery is synchronous REST today.
- Persistence: PostgreSQL (`org.postgresql:postgresql`, runtime, with `flyway-database-postgresql`) against the
  shared `sky` database, versioned with Flyway (`src/main/resources/db/migration/`, now V1 and V2). Tests run
  against a Testcontainers `postgres:17-alpine` container, never an in-memory database. H2 is gone from this module,
  from the shared test stack, and from the version catalogue.
- `POSTGRES_USER` and `POSTGRES_PASSWORD` have no default in `application.yaml` and must not gain one: the
  `spring-boot-hygiene` specification grants its credential-default exemption to `local` profile files alone, and
  `application-local.yaml` already defaults both. An unset variable now fails startup through
  `DatasourceCredentialsAutoConfiguration` in sky-common, naming the variable and the property, instead of
  binding the literal placeholder text as the password. `DatasourceCredentialsStartupTest` pins that behaviour
  against this module's own configuration files, so do not delete it when touching the datasource block.
- API docs: springdoc `webmvc` UI at `/swagger-ui/index.html`. The hand-written contract in
  `docs/api/openapi/sky-message.openapi.yaml` is maintained by hand, so a change to the wire shape of an endpoint
  changes both together. Send answers 201, 400, 401 and 403 and nothing else: there is no 404 and no 503 on it.

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit). Integration and
repository tests use Testcontainers PostgreSQL only (no Kafka container). `MessageRepositoryDataJpaTest` is a
`@DataJpaTest` slice against that same container, enabled and passing: nothing in this module is `@Disabled`. Run from
the repo root, e.g. `./gradlew :sky-message:test`.

Nothing here needs a live identity provider. The `test` profile carries no profile-specific file at all: there is no
`src/test/resources`, because the only thing it ever held was the directory client secret the removed startup check
demanded. `TestSecurityConfig` supplies a stub `JwtDecoder` that reads the bearer token value as the `email` claim, so
a context-loading test needs no reachable issuer and no environment variable.

`jacocoTestCoverageVerification` runs as part of `check` at 0.90 line and 0.90 branch over the measured set, and the
module sits at 1.00 on both. A change here that ships without a test lowers that, so add the test with the change.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: adapters depend inward on `domain/ports/inbound` and `domain/ports/outbound`;
  `domain/service` implementations depend on those port interfaces. Adapters must never import from `domain/service`
  directly. ArchUnit enforces this.
- Four more rules landed in `HexagonalArchitectureTest` alongside the older ones, and each was proved to fail on a
  deliberate violation before it was trusted. No class under `adapters.inbound` may reach a Spring Data repository or
  a `Repository`-suffixed interface, so a controller cannot skip the domain service. Every class under `domain` may
  reach only the packages an allow list in that test names, which is the persistence and validation API the model
  genuinely carries plus Lombok, SLF4J and a short set of Spring annotations, and nothing else. The one
  `@RestControllerAdvice` is carved out by annotation and bounded to that list plus `org.springframework.http`, the
  package `org.springframework.web` itself and `org.springframework.web.bind.annotation`, so it cannot reach an HTTP
  client. Nothing may reach another service's classes. A new framework dependency in the domain therefore fails the
  build: admit it in `openspec/specs/architecture/spec.md` through the change workflow first, and never by adding an
  exclusion to the test.
