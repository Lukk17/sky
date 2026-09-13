# Changelog: sky-offer

All notable changes to this module are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to
[Semantic Versioning](https://semver.org/). Newest entry on top. The topmost `## [x.y.z]`
version is the current one. The release workflow reads it for the image tag and to guard
against re-publishing an already-released version, so keep it at the top and bump it before
every release. The `version` in `build.gradle.kts` is a cosmetic label the release workflow
does not read. If the two disagree, this file wins for release purposes.

## [2.0.0]

Major. Every write path moved to a new URL, the identifier type changed, and the error
contract changed. A client built against 1.x will not work against it.

### Changed
- Offer identifiers are UUIDs, not numbers. `Offer.id` was a database-generated `Long`. It is
  a `UUID` in the entity, in `OfferDTO`, in `OfferEditDTO`, and in every path variable that
  names an offer. This breaks the Sky-View frontend on its own, and it breaks the `offerId`
  values sky-booking has stored, which is why both services ship 2.0.0 together. There is no
  migration from the old numeric ids, because the underlying database changed with them.
- The owner write paths are plural. `POST /owner/offer` is `POST /owner/offers`,
  `PUT /owner/offer` is `PUT /owner/offers`, and `DELETE /owner/offer/{offerId}` is
  `DELETE /owner/offers/{offerId}`. The old singular paths are gone, not redirected, so a
  client that was not updated gets 404 on every write.
- The internal owner lookup is `GET /offers/{offerId}/owner`, not
  `GET /owner/offer/{offerId}`. The old path read as an owner-scoped collection when the
  resource being addressed is the offer, and it sat in a second controller that is now merged
  into the one API controller. sky-booking 2.0.0 calls the new path.
- The database is PostgreSQL, not MySQL. The driver is `org.postgresql.Driver`, the schema is
  owned by Flyway (`V1__init.sql` creating `offer` and `offer_event`, `V2` adding the owner
  index and the event sequence constraint), and `hibernate.ddl-auto` is `validate` so the
  application refuses to start against a schema its entities do not match. The Flyway history
  table is `flyway_schema_history_offer`. The `price` column is `NUMERIC(12,2)` rather than a
  floating point column, so a price no longer drifts by fractions of a cent.
- Authentication is a Keycloak-issued JWT this service validates itself, not a header the edge
  was trusted to set. 1.x read the caller identity out of a request header map, so anything
  that could reach the port could claim to own any offer. The service is now an OAuth2
  resource server validating against `OAUTH2_ISSUER_URI` with an audience check, realm roles
  become authorities, and every owner-scoped endpoint carries `@IsUser`. Ownership is checked
  against the token identity rather than against an email in the request body.
- Listing and search return a page, not an array. `GET /offers`, `GET /owner/offers` and
  `POST /search` returned bare JSON arrays of every matching row. All three return a Spring
  `Page` now, default size 20, and accept `page`, `size` and `sort`. A caller reads the items
  out of `content` and the total out of `totalElements`.
- Search filters in the database rather than in the application. `POST /search` loaded every
  offer and filtered the list in memory, so both the response time and the heap cost grew with
  the table. The predicate is a query now. The search term is validated: blank is 400 and
  anything over 100 characters is 400, where 1.x accepted both.
- Error responses are RFC 9457 problem documents, not free text. A missing offer is 404 where
  1.x answered 200 with an empty body or 400 with a message string, a validation failure is 400
  with a `field-errors` object naming each rejected field, and an unauthorized caller is 401.
  The internal owner lookup documented a 402 for a missing caller identity, which was never a
  meaningful status for it, and answers 400 or 401 now.
- `DELETE /owner/offers/{offerId}` answers 204 with no body, where it answered 200 with a
  JSON-encoded confirmation string.
- `photoPath` is gone from every request and every response, and the one column it used to be is
  now two. The server owns where a photo lives: `photo_object_key` holds the object-storage key,
  is written only by the photo upload and cleared only by the photo delete, and never appears on
  the wire. `external_photo_url` holds the address of an image hosted elsewhere, is the one photo
  field a client may write, and has to be an absolute `http` or `https` URL, so a storage key
  cannot be stored in it. A client asks for the offer and reads `photoUrl`, which the service
  derives per response: the presigned URL of the stored object when there is one,
  `external_photo_url` otherwise, and null when there is neither. A request body that still
  carries `photoPath` is accepted and the field is discarded, so Sky-View gets a 200 rather than a
  400 while it is updated, and the value it sends has no effect at all. Sort expressions naming
  `photoPath` now return 400: the sortable names are `photoObjectKey` and `externalPhotoUrl`.
- JSON is Jackson 3 (`tools.jackson`), not Gson, so field order and null handling in the Kafka
  event payload can differ from 1.x output.
- Spring Boot 4.0.7 on Java 25, built by Gradle 9.6.1 as a module of the composite build. The
  per-module wrapper, `settings.gradle.kts` and Gradle directory are gone: build from the
  repository root with `./gradlew :sky-offer:test`. Dependency versions come from
  `gradle/libs.versions.toml` and shared build logic from the `buildSrc` convention plugins.

### Added

- A missing `POSTGRES_USER` or `POSTGRES_PASSWORD` now fails startup with a message naming the
  variable and the property it feeds, instead of starting with the literal text `${POSTGRES_PASSWORD}`
  as the password and failing later on a database authentication error that names neither. The check
  arrives from sky-common as `DatasourceCredentialsAutoConfiguration` and needs no wiring here.
  `DatasourceCredentialsStartupTest` pins the three cases against this module's own configuration
  files: both variables supplied starts, either one absent fails with the new message, and the `local`
  profile starts with no variables set at all because its own file defaults them.
- The object store credentials no longer carry a development default in `application.yaml`.
  `S3_ACCESS_KEY` defaulted to `root` and `S3_SECRET_KEY` to `localdev` in a file with no profile in
  its name, which the `spring-boot-hygiene` specification forbids: the one exemption it grants is for
  a file whose own name carries the `local` profile. Both are bare `${S3_ACCESS_KEY}` and
  `${S3_SECRET_KEY}` placeholders now, and the two development values moved to
  `application-local.yaml`, where the exemption applies and where a local run still picks them up with
  nothing exported. `S3Config` validates both through `RequiredCredentials` before it builds the S3
  client, so an unset variable on the default profile fails the boot naming `sky.s3.secret-key` and
  `S3_SECRET_KEY` rather than producing a 403 from the store on the first upload. Nothing else
  depended on the removed defaults: both compose files set the two variables explicitly, the Helm
  chart takes them from the `sky-secrets` secret, and the test configuration sets its own values.
- `S3_PRESIGN_ENDPOINT` (`sky.s3.presign-endpoint`) separates the address the service uploads to
  from the address a presigned URL names. `S3_ENDPOINT` stays the first, and keeps the whole of
  its old meaning for every S3 call this service makes. The new variable is the second, and it is
  what a browser or a test runner has to resolve. Unset or blank, the presigner falls back to
  `S3_ENDPOINT`, so docker-compose and the test suite behave exactly as before. A cluster sets it,
  because the internal service name the upload uses resolves for no client outside the cluster, so
  every photo URL a cluster produced before this was unreachable.
- `DELETE /owner/offers/{offerId}/photo` removes the stored object and clears the offer's record of
  it, for the owner only, answering 204. It is the only way to clear a photo, because an edit
  cannot reach the key, and it is idempotent: an offer with no photo answers 204 and touches
  nothing. A second upload already replaced the first object rather than leaving it behind, and
  deleting an offer already removed its object; with the key server-owned, both now hand the
  storage adapter a key its prefix guard accepts rather than whatever a client last typed.
- `V3__split_photo_object_key_from_external_photo_url.sql` and
  `V4__classify_existing_photo_values.sql`, one change in two files because DDL and DML never share
  a migration. V3 adds `photo_object_key VARCHAR(512)`, renames `photo_path` to
  `external_photo_url` and widens it to 1024. V4 decides what each pre-split value becomes: a value
  already inside that offer's own `offers/{offerId}/` prefix is adopted as the object key, an
  absolute `http` or `https` URL survives as the external address, and everything else is dropped,
  including a key the storage prefix guard would refuse anyway, such as one belonging to another
  offer or one written before the per-offer prefix existed.
- Photo storage on S3 through AWS SDK v2, against floci both locally and in a cluster.
  `POST /owner/offers/{offerId}/photo` takes a multipart upload, and the
  returned `OfferDTO` carries a `photoUrl` presigned for a configured time to live. The upload
  validates the content type from the bytes rather than from the declared header or the file
  extension, so renaming a file does not get it past the check, and an empty upload is 400.
  1.x had no photo endpoint at all and stored only a `photoPath` string that nothing served.
- Hexagonal layout with the dependency direction enforced by ArchUnit. `domain/ports/inbound`
  holds what controllers call, `domain/ports/outbound` holds the repository, event store,
  notification and `PhotoStorage` interfaces that adapters implement, and `domain/service`
  holds the implementations. An adapter importing from `domain/service` fails the build rather
  than passing review.
- Testcontainers PostgreSQL and Kafka integration tests through `@ServiceConnection`, an
  ArchUnit layering suite, and unit coverage over the S3 adapter.
- Connection pool sizing and a durable Kafka producer. 1.x ran pool defaults and a fire and
  forget producer, so a broker restart dropped offer events silently. All five
  delivery-guarantee properties are now set explicitly and none is left to a client default:
  `acks=all`, `enable.idempotence=true`, `retries=2147483647`, `delivery.timeout.ms=120000` and
  `max.in.flight.requests.per.connection=5`. The three that were missing all equal the current
  kafka-clients default, which is exactly why they are written down: a default that moves
  between client versions must not be able to change the durability of a write without a line of
  this repository changing. `retries` is a pin rather than a guarantee, because the delivery
  timeout is what actually bounds retrying, and the module documentation says so rather than
  implying a large number is tuning.
- `KafkaProducerDeliveryGuaranteeTest`, which asserts the five properties on the resolved
  `ProducerFactory.getConfigurationProperties()` map the booted context builds rather than on the
  YAML that feeds it, and builds a real `KafkaProducer` from that map so the two client startup
  validations run: the idempotent producer's in-flight ceiling of 5, and the requirement that
  `delivery.timeout.ms` be at least `linger.ms + request.timeout.ms`.
- A demo data seed, gated to the `local` profile through a separate Flyway location, so a fresh
  local stack has offers to look at without a manual insert.
- A local profile that starts without Keycloak, a per-service startup banner, and a structured
  startup log line naming the deployment it thinks it is in.

### Removed
- The `GET /` and `GET /home` probe endpoints, which returned a configured greeting string and
  published a Kafka notification as a side effect of being scraped. Health now lives at
  `/actuator/health`, with separate liveness and readiness probes.
- `OfferInternalController`, merged into `OfferApiController`, and the Elastic Beanstalk
  `Dockerrun.aws.json` left over from a deployment target this project no longer has.
- The MySQL driver and the module copies of `KafkaPayloadModel`, `SwaggerConfig` and the three
  `propertyBind` classes, all of which now come from sky-common.

### Fixed
- An unreachable object store answered 400 Bad Request, telling a caller its photo upload was
  malformed when the truth was that a dependency was down, and telling every client not to retry at
  the one moment retrying was right. `S3PhotoStorage` wrapped every `SdkException` in
  `OfferException`, which the advice maps to 400. It now classifies the failure instead and throws
  one of two new types, neither of which extends `OfferException`, so neither can drift back onto
  the 400 mapping. A store that could not be used (a refused connection, a connect timeout, a read
  timeout, a 5xx, a 429, or a photo address that could not be signed) is
  `PhotoStorageUnavailableException`, mapped to 503 Service Unavailable with `Retry-After: 10`. A
  store that answered and answered unusably (401 or 403 from wrong credentials, or a missing bucket)
  is `PhotoStorageBadResponseException`, mapped to 502 Bad Gateway with no `Retry-After`, because
  repeating the request changes nothing. Both bodies are RFC 9457 problem details. This is the shape
  sky-booking already uses for its one outbound hop, so the whole repository now answers a dependency
  outage the same way. What stays 4xx is what a caller can fix: an empty `file` part and an upload
  that is not a JPEG, PNG, GIF or WebP are still 400, and an oversized upload is still 413. Photo
  deletion, on the photo path and as part of deleting an offer, still logs `photo_delete_failed` and
  answers as it would on a reachable store, so an outage cannot leave an owner unable to delete.
- A failing upload echoed the object store's own words back to the caller. The 400 `detail` was
  built by concatenating the SDK message, so a wrong-credentials failure answered
  `Photo upload failed: Access Denied (Service: S3, Status Code: 403, Request ID: null)`. The
  `detail` is now one of a fixed set of sentences and the store's status goes to the log instead,
  under `photo_storage_call_failed` with an `operation` field.
- A client could overwrite the server's record of where an offer's photo was stored, and every
  delete after that leaked the object. The upload wrote the key it had built into `photoPath`, and
  then an edit overwrote that same field with whatever string the payload carried, because
  `photoPath` was both the storage key and a free-text field. Deleting the offer handed the
  overwritten value to the storage adapter, whose prefix guard correctly refused a key outside
  `offers/{offerId}/` and logged `photo_delete_skipped`, and the real object stayed in the bucket
  with nothing left pointing at it. The guard was never the defect and it stays exactly as it was.
  The column a client can write and the column the storage layer reads are now two different
  columns, and nothing maps a request field onto the second one.
- Two writes to the same offer could record the same event sequence number, because the
  sequence was read and then written with nothing enforcing uniqueness in between. The
  `uq_offer_event_offer_seq` unique constraint now enforces it and the appender retries on the
  conflict, so the event stream for an offer is gapless and ordered.
- An exhausted event append retry escaped with no advice mapping, so it surfaced as a bare 500
  carrying Spring Boot's default error body instead of the RFC 9457 problem document every
  other error here returns. `EventSequenceConflictException` is now mapped to 409 Conflict on
  the three write operations that append an event (`POST /owner/offers`, `PUT /owner/offers`
  and `DELETE /owner/offers/{offerId}`), because the write lost a race on a sequence number
  rather than failing on the server. It sends no `Retry-After` on purpose: the conflict means
  another writer advanced the sequence, so waiting achieves nothing and the `detail` says to
  re-read the offer before retrying. That `detail` is a fixed sentence rather than the internal
  message, so nothing about the event log leaks to the caller. This matches the same fix in
  sky-booking, which carried the identical defect.
- Event timestamps were stored in a type with no time zone, so a reader in a different zone
  read a different instant than the writer wrote. The column is `timestamptz` and the field is
  an `Instant`.
- Editing an offer with a partial payload overwrote the omitted fields with null, because the
  incoming DTO was persisted as given. An omitted field now keeps its stored value, and only
  the fields actually present are changed.
- Flyway did not actually run under Spring Boot 4, so the schema was whatever Hibernate had
  last generated. It runs now, with `baseline-on-migrate` and baseline version 0 so an existing
  database adopts the history without a manual repair.
