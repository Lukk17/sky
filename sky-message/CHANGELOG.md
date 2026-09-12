# Changelog: sky-message

All notable changes to this module are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to
[Semantic Versioning](https://semver.org/). Newest entry on top. The topmost `## [x.y.z]`
version is the current one. The release workflow reads it for the image tag and to guard
against re-publishing an already-released version, so keep it at the top and bump it before
every release. The `version` in `build.gradle.kts` is a cosmetic label the release workflow
does not read. If the two disagree, this file wins for release purposes.

## [2.0.0]

Major. The send and delete paths moved, the identifier type changed, and the error contract
changed. A client built against 1.x will not work against it.

### Changed
- Message identifiers are UUIDs, not numbers. `Message.id` was a database-generated `Long`. It
  is a `UUID` in the entity, in `MessageDTO`, and in the `DELETE /messages/{messageId}` path
  variable. This breaks the Sky-View frontend on its own. There is no migration from the old
  numeric ids, because the underlying database changed with them.
- The send and delete paths are plural. `POST /message` is `POST /messages` and
  `DELETE /message/{messageId}` is `DELETE /messages/{messageId}`. The old singular paths are
  gone, not redirected, so a client that was not updated gets 404. `GET messages/sent` was also
  registered without a leading slash, which worked by accident, and is `GET /messages/sent`.
- The database is PostgreSQL, not MySQL. The driver is `org.postgresql.Driver`, the schema is
  owned by Flyway (`V1__init.sql` creating `message`, `V2` adding the sender and receiver
  indexes), and `hibernate.ddl-auto` is `validate` so the application refuses to start against a
  schema its entities do not match. The Flyway history table is
  `flyway_schema_history_message`, so the three services sharing the `sky` schema each version
  their own tables instead of fighting over one history table. The `text` column is `TEXT`
  rather than a bounded string, with the 65000 character limit enforced by validation.
- Authentication is a Keycloak-issued JWT this service validates itself, not a header the edge
  was trusted to set. 1.x read the caller identity out of a request header map, so anything that
  could reach the port could read or delete anyone else's messages. The service is now an OAuth2
  resource server validating against `OAUTH2_ISSUER_URI` with an audience check, realm roles
  become authorities, and every endpoint carries `@IsUser`. The sender on a new message is taken
  from the token rather than from the request body, so a caller can no longer send a message as
  somebody else.
- The inbox endpoints return a page, not an array. `GET /messages/received` and
  `GET /messages/sent` returned every message the user had in one bare JSON array. Both return a
  Spring `Page` now, default size 20, and accept `page`, `size` and `sort`. A caller reads the
  items out of `content` and the total out of `totalElements`.
- Error responses are RFC 9457 problem documents, not free text. Deleting a message that does
  not exist is 404, deleting one that belongs to somebody else is 403, a validation failure is
  400 with a `field-errors` object naming each rejected field, and an unauthorized caller is 401.
  Anything parsing the old message strings has to be rewritten.
- `DELETE /messages/{messageId}` answers 204 with no body, where it answered 200 with a
  JSON-encoded confirmation string.
- Spring Boot 4.0.7 on Java 25, built by Gradle 9.6.1 as a module of the composite build. The
  per-module wrapper, `settings.gradle.kts` and Gradle directory are gone: build from the
  repository root with `./gradlew :sky-message:test`. Dependency versions come from
  `gradle/libs.versions.toml` and shared build logic from the `buildSrc` convention plugins.

### Added
- Hexagonal layout with the dependency direction enforced by ArchUnit. `domain/ports/inbound`
  holds what the controller calls, `domain/ports/outbound` holds the repository interface the
  adapter implements, and `domain/service` holds the implementation. An adapter importing from
  `domain/service` fails the build rather than passing review.
- Testcontainers PostgreSQL integration tests through `@ServiceConnection`, a Spring Data JPA
  slice test over the repository, and an ArchUnit layering suite. No Kafka container, because
  this service still has no Kafka dependency.
- Connection pool sizing, and indexes on `sender_email` and `receiver_email`. Both inbox queries
  previously scanned the table.
- A demo data seed, gated to the `local` profile through a separate Flyway location, so a fresh
  local stack has a conversation to look at without a manual insert.
- A local profile that starts without Keycloak, a per-service startup banner, and a structured
  startup log line naming the deployment it thinks it is in.

### Removed
- Receiver validation on send, and with it every trace of this service acting as an OAuth2 client.
  `POST /messages` briefly asked Keycloak's administration interface whether `receiverEmail` named a
  realm user, and refused the send with 404 when it did not. The check is gone: a send to an address
  nobody owns is accepted as 201 again, which is the accepted consequence. What went with it is the
  point. A business service needed a `realm-management` `view-users` grant, a client secret in
  `USER_DIRECTORY_CLIENT_SECRET`, and an outbound call on a write path that turned a slow Keycloak
  into a 503 on sending a message. It also let any authenticated caller discover which addresses the
  realm holds, one send at a time. On the cluster the call never completed at all: the ingress held
  the request for 120 seconds and answered 400 with an empty body, so the first send took about 128
  seconds and ended as a 504, and later sends failed fast on the open circuit. Deleted with it:
  `UserDirectory`, `KeycloakUserDirectory`, `KeycloakDirectoryCaller`, `ReceiverNotFoundException`,
  `UserDirectoryUnavailableException`, `UserDirectoryClientConfig`, `RestClientConfig` and the
  `RestClient` bean nothing else used, `SkyConfigProperties`, the `spring.security.oauth2.client`
  blocks, the `userDirectory` Resilience4j retry and circuit breaker instances, the
  declared `spring-boot-starter-oauth2-client`, `resilience4j` and `aspectjweaver` dependencies, and
  the `view-users` grant in `config/k8s/helm/infra/keycloak/files/sky-realm.json`. Nothing named
  `oauth2-client` is left on the runtime classpath, while `aspectjweaver` still arrives transitively
  through `spring-boot-starter-data-jpa` and is nobody's to remove here. The ArchUnit rule that
  confined the OAuth2 client package to `adapters/outbound` and `config` was kept and widened to
  forbid the package outright, so it now guards the decision instead of a layering boundary. JWT
  validation against `OAUTH2_ISSUER_URI` is untouched, and it is the only reason this service still
  talks to Keycloak.
- The 503 with `Retry-After` on send, and the 404 for an unknown receiver. Neither status can occur
  any more, and both are gone from `docs/api/openapi/sky-message.openapi.yaml`. Send answers 201,
  400, 401, 403, 415 or 500.
- The `GET /` and `GET /home` probe endpoints, which returned a configured greeting string.
  Health now lives at `/actuator/health`, with separate liveness and readiness probes.
- The MySQL driver, the Elastic Beanstalk `Dockerrun.aws.json` left over from a deployment
  target this project no longer has, and the module copies of `SwaggerConfig`, `Constants` and
  the three `propertyBind` classes, all of which now come from sky-common.

### Fixed
- Deleting a message that did not exist answered as though it had been deleted, because the
  delete was issued without first reading the row. A missing message is 404 now, and a message
  owned by somebody else is 403, so a caller can tell the two apart.
- `MessageDTO.read` was a primitive `boolean`, which Jackson 3 cannot leave absent. A partial
  payload that omitted the field failed to deserialize rather than defaulting. The field is a
  nullable `Boolean`.
- Message timestamps were stored in a type with no time zone, so a reader in a different zone
  read a different instant than the writer wrote. The column is `timestamptz` and the field is
  an `Instant`.
- Flyway did not actually run under Spring Boot 4, so the schema was whatever Hibernate had last
  generated. It runs now, with `baseline-on-migrate` and baseline version 0 so an existing
  database adopts the history without a manual repair.
