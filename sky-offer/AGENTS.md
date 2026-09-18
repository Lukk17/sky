# AGENTS.md: sky-offer

Module-local guidance for `sky-offer`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-offer` is the offers service: CRUD over flight/booking offers, the inventory the other services book against. It runs
on port 5552 (`OFFER_PORT`, default 5552), exposes REST under the `/api/v1` prefix, and is one of the four
independently-deployable services. The module version is not restated here: the topmost `## [x.y.z]` entry in
[CHANGELOG.md](CHANGELOG.md) is the current one, and it wins over the cosmetic `version` in `build.gradle.kts`.

## Architecture

- Build plugin: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-offer.jar`, JaCoCo
  report). Depends on `:sky-common`.
- Hexagonal layout (`com.lukk.sky.offer`):
  - `domain/model`, `domain/exception`: core model and domain exceptions.
  - `domain/ports/inbound`: driving port interfaces called by controllers (`OfferService`).
  - `domain/ports/outbound`: driven port interfaces implemented by infrastructure adapters (`OfferRepository`,
    `EventSourceRepository`, `OfferNotificationService`, `PhotoStorage`).
  - `domain/service`: domain service implementations (`OfferServicePrimary`, `EventSourceService`,
    `EventSourceServicePrimary`).
  - `adapters/inbound/api`: REST controllers; `adapters/dto`: wire DTOs and the `@NullOrNotBlank` and
    `@ExternalPhotoUrl` constraints; `adapters/outbound/notification`: outbound notification;
    `adapters/outbound/storage`: the S3 photo storage adapter.
  - `config`, `config/kafka`, `config/propertyBind`: Spring wiring and bound properties.
- MVC stack: plain Spring Web (`spring-boot-starter-web`), no WebFlux. `sky-booking` finished its WebFlux
  removal too, so `sky-gateway` is now the only module on the reactive stack.
- Photo storage speaks to two addresses, and conflating them is the defect this module already had.
  `sky.s3.endpoint` (`S3_ENDPOINT`) is where `S3Client` sends upload, delete and bucket creation.
  `sky.s3.presign-endpoint` (`S3_PRESIGN_ENDPOINT`) is the only thing `S3Presigner` is built against, and it is
  what a client outside the deployment has to resolve. `S3Properties.resolvedPresignEndpoint()` falls back to
  `endpoint` when the presign endpoint is null or blank, which is what keeps docker-compose and the test suite
  on their pre-existing behaviour: there one hostname, `s3.localhost:9070`, resolves inside the container and on
  the host alike, so the two addresses genuinely coincide. In a cluster they do not, and
  `config/k8s/helm/service/sky-offer/values-local.yaml` sets the presign endpoint to `http://s3.localhost:5777`,
  the floci ingress. Never point the presigner at a cluster-internal service name: the upload succeeds and no
  client can ever read the photo back, which is exactly the bug that was shipped before.
  A presigned URL is signed over the host it names, port included, so the signed host must be the host the client
  dials and the store receives. nginx forwards the client `Host` verbatim, so the ingress hop is safe.
  The object store is floci, the same AWS emulator and the same pinned image digest as the local developer stack,
  installed by `config/k8s/helm/infra/floci`. It authenticates nobody and verifies no signature, so
  `S3_ACCESS_KEY` and `S3_SECRET_KEY` exist only because the AWS SDK refuses to build a client without them.
  Neither carries a default in `application.yaml`, because that file has no profile in its name and the
  `spring-boot-hygiene` specification grants its one credential-default exemption to `local` profile files alone.
  The development values live in `application-local.yaml` instead, so a `local` run still needs nothing exported.
  `S3Config` runs both through `RequiredCredentials` from sky-common before building the client, so an unset
  variable on any other profile fails the boot naming the property and the variable rather than turning into a
  store rejection on the first upload. Do not put the defaults back in `application.yaml`, and do not add a
  default to a new credential anywhere except a `local` profile file.
- Photo keys are namespaced as `offers/{offerId}/{uuid}-{filename}`, and `S3PhotoStorage.delete` refuses any
  key outside the prefix for the offer it was handed. That closes a cross-tenant deletion hole, so do not relax it.
- Persistence: PostgreSQL (`org.postgresql:postgresql`, runtime, with `flyway-database-postgresql`) against the
  shared `sky` database, versioned with Flyway (`src/main/resources/db/migration/`, now V1 to V4). Tests run
  against a Testcontainers `postgres:17-alpine` container, never an in-memory database. H2 is gone from this module,
  from the shared test stack, and from the version catalogue. V3 and V4 are one change in two files because DDL and
  DML never share a migration: V3 adds `photo_object_key`, renames `photo_path` to `external_photo_url` and widens
  it, and V4 classifies the rows written before the split. `PhotoColumnMigrationTest` drives both against a
  Testcontainers Postgres, with rows inserted at V2, and pins what each kind of old value becomes.
- `POSTGRES_USER` and `POSTGRES_PASSWORD` have no default in `application.yaml` and must not gain one: the
  `spring-boot-hygiene` specification grants its credential-default exemption to `local` profile files alone, and
  `application-local.yaml` already defaults both. An unset variable now fails startup through
  `DatasourceCredentialsAutoConfiguration` in sky-common, naming the variable and the property, instead of
  binding the literal placeholder text as the password. `DatasourceCredentialsStartupTest` pins that behaviour
  against this module's own configuration files, so do not delete it when touching the datasource block.
- The server owns the photo key and a client never sees it. `photo_object_key` is written only by
  `POST /owner/offers/{offerId}/photo` and cleared only by `DELETE` on that same path. It is absent from `OfferDTO`
  and `OfferEditDTO`, so no request body reaches it, and `OfferEditDTO.applyTo` mutates the loaded entity field by
  field without naming it or the owner. Three rules hold here:
  - A client addresses the photo by the offer identifier, and reads it back as `photoUrl`, which the service
    derives per response: the presigned URL of the stored object when there is one, `external_photo_url` otherwise,
    null when neither. Nothing presigns a value that arrived in a request.
  - `external_photo_url` is the one photo field a client may write, for an image hosted elsewhere that nobody
    uploaded, which is what the demo seed rows hold. `@ExternalPhotoUrl` requires an absolute `http` or `https` URL,
    so its value space and the `offers/{offerId}/...` key namespace are disjoint. Do not relax that constraint and do
    not add a second writable photo field.
  - Every object the bucket holds is reachable from a row, or it is a leak. A second upload deletes the object it
    replaced, deleting the offer deletes its object, and the photo delete endpoint is the only way to clear one. The
    prefix guard in `S3PhotoStorage.delete` stays: it refuses any key outside `offers/{offerId}/`, which is why V4
    adopts a pre-split value only when it already sits in that offer's own prefix and drops it otherwise. A key the
    guard would refuse is not a key this service can manage.
  - Known residual, accepted, not mitigated: both storage deletions run inside the transaction that commits the
    row change, so a commit failure in the instant after the delete leaves the object gone and the row still naming
    it. The window is one commit wide and the alternative is an after-commit listener nobody asked for. Do not
    reorder the delete to run before the save, which would widen the window rather than close it.
- `PUT /owner/offers` is a partial update, and absent means keep the stored value. `OfferEditDTO.applyTo`
  writes a field only when the payload supplies one, through the `applyIfSupplied` helper, and it names neither
  `photoObjectKey` nor `ownerEmail`, so neither is reachable from a request body. Two rules hold here:
  - Do not merge through `Objects.requireNonNullElseGet`. That was the previous shape and it threw a
    `NullPointerException` whenever the stored value was also null, which is the exact case a partial payload
    reaches: the supplier form of that method rejects a null result. `description`, `comment` and
    `external_photo_url` are the three nullable columns the merge touches, so those three carried the defect,
    and the failure surfaced as a bare 500 with Spring Boot's default error body because nothing maps a
    `NullPointerException` to a problem detail. `OfferEditDTOTest` pins all three, and
    `OfferIntegrationTest.updateOffer_whenPayloadIsPartialAndTheStoredRowIsSparse_thenReturn200AndKeepTheAbsentFieldsNull`
    pins it over the whole HTTP stack, because the Bruno collection only ever sends full payloads against fully
    populated rows and so cannot see this class of defect.
  - Nothing can be cleared back to null through this endpoint, and that is a known contract gap rather than an
    oversight. An explicit null is indistinguishable from an omission, `description` and `comment` accept an empty
    string and store an empty string, and `externalPhotoUrl` accepts neither, because `@ExternalPhotoUrl` demands
    an absolute URL. Closing it means a tri-state payload shape, which is a contract decision the owner has not
    taken: do not add one field-clearing convention on its own.
- The domain service publishes the Kafka event, never the controller. `OfferServicePrimary` calls
  `OfferNotificationService` at the end of `addOffer`, `editOffer` and `deleteOffer`, and `OfferApiController` injects
  no outbound port at all. It used to inject one, build the `KafkaPayloadModel` itself, serialise the DTO and format
  the delete sentence, which put the decision to announce an offer one layer above the rules that decide whether the
  offer changed. The driven port now names the event (`publishCreated`, `publishEdited`, `publishDeleted`) and the
  adapter owns the envelope, the timestamp, the serialisation and the `Offer with ID: %s was deleted.` sentence, so
  `KafkaPayloadModel` and the `ObjectMapper` are gone from both the controller and the domain. The port takes
  `OfferDTO` rather than `Offer` on purpose: `photoUrl` is derived per response by `OfferServicePrimary.toDto`, which
  presigns the stored object, so an adapter mapping the entity itself would either drop that field from the wire
  payload or call `PhotoStorage` from inside a second driven adapter.
- Moving the call put all three publications inside the class-level `@Transactional` on `OfferServicePrimary`, where
  they used to run after the commit. The residual that creates is the mirror of the old one. Before, a committed write
  could end up with no event, because `KafkaNotificationPublisher` logs a failed send at warn and swallows it. Now, an
  event can be handed to the producer and the commit can then fail, leaving an event naming an offer that does not
  exist or naming one that was never deleted. The window is one commit wide, and it is the same family as the
  accepted storage residual recorded above, where both storage deletions run inside the transaction that commits the
  row change. The fix for it is a transactional outbox that nobody has asked for. Keep the publish last in the method,
  after the event-store append and after the storage cleanup, so anything the domain rejects still publishes nothing.
  Do not move it earlier, which widens the window rather than closing it.
- Messaging: produces/consumes Kafka events via `spring-kafka`, serialised with the Spring-managed Jackson 3
  `ObjectMapper` (`tools.jackson.databind.ObjectMapper`). Gson is gone from this module and from its build file.
- All five producer delivery-guarantee properties are set explicitly in `application.yaml` and none is left to a
  client default: `acks: all` and `retries: 2147483647` as dedicated Spring Boot keys, and
  `enable.idempotence: true`, `delivery.timeout.ms: 120000` and `max.in.flight.requests.per.connection: 5`
  under `producer.properties`, because Spring Boot exposes no dedicated key for those three.
  Three of the five interact, and none of the numbers is a free choice:
  - `max.in.flight.requests.per.connection` is 5 because that is the ceiling the idempotent producer enforces.
    `ProducerConfig.postProcessAndValidateIdempotenceConfigs` in kafka-clients 4.1.2 throws
    `ConfigException("To use the idempotent producer, max.in.flight.requests.per.connection must be set to at most
    5")` above that, so 6 fails producer construction instead of misbehaving at run time. Dropping to 1 would cost
    throughput for nothing: the idempotent producer preserves order at any permitted value, which is what the
    client's own `enable.idempotence` documentation says.
  - `delivery.timeout.ms` is 120000 because the client requires it to be at least `linger.ms + request.timeout.ms`
    and `KafkaProducer.configureDeliveryTimeout` throws `ConfigException("delivery.timeout.ms should be equal to or
    larger than linger.ms + request.timeout.ms")` for an explicitly set value below that sum. Nothing in this
    repository sets either of those two, so their 4.1.2 defaults apply, 5 and 30000, and the floor is 30005. Note
    that `linger.ms` defaulted to 0 before Kafka 4.0, so the floor moved. Setting `linger.ms` or
    `request.timeout.ms` here means rechecking that sum.
  - `retries` is a pin rather than a guarantee, and saying so is the point. With `delivery.timeout.ms` set, that
    timeout is the real bound on how long a send is retried, and the client documentation says to leave `retries`
    unset and control retry behaviour through the timeout instead. The only behaviour the value still carries is
    that an idempotent producer refuses 0, so every non-zero number behaves identically and a large one is not
    tuning. It is set because the specification requires all five to be explicit and because a client default that
    moved to 0 would silently disable idempotence.
  All three of the values added here happen to equal the kafka-clients 4.1.2 default, which is exactly why they are
  written down: a default that moves between client versions must not be able to change the durability of a write
  without a line of this repository changing.
  `KafkaProducerDeliveryGuaranteeTest` asserts the resolved `ProducerFactory.getConfigurationProperties()` map
  the booted context builds, not the configuration source that feeds it, and builds a real `KafkaProducer` from
  that map so both validations above actually run. sky-booking and sky-notify carry the identical five values, so
  change all three or none.
- A lost race on the event sequence is 409 here, not a bare 500. `EventSequenceConflictException` is thrown by
  `EventSourceServicePrimary` once twenty append attempts have all lost the sequence number, and
  `GlobalExceptionHandler` maps it to 409 Conflict on the three operations that append an event (create, edit and
  delete). It had no advice mapping before, so it escaped as Spring Boot's default 500 error body while every other
  error in this repository came back as an RFC 9457 problem detail. It carries no `Retry-After` on purpose: waiting
  does not resolve a conflict whose cause is another writer advancing the sequence, so the fixed `detail` tells the
  caller to re-read the offer before retrying. sky-booking carries the identical mapping for the identical defect,
  so change both together.
- An object store failure is 503 or 502 here, never 400. `S3PhotoStorage` classifies every
  `SdkException` it catches and throws one of two types, neither of which extends `OfferException`:
  `PhotoStorageUnavailableException` for a store that could not be used (a refused connection, a connect or read
  timeout, a 5xx, a 429, or a presign that failed), which `GlobalExceptionHandler` maps to 503 with
  `Retry-After: 10`, and `PhotoStorageBadResponseException` for a store that answered unusably (401, 403, or a missing
  bucket), which maps to 502 with no `Retry-After`. The split from `OfferException` is the load-bearing part: that type
  maps to 400, so anything inheriting from it drifts back to telling a caller its request was malformed during an
  outage. This mirrors `OfferServiceUnavailableException` and `OfferServiceBadResponseException` in sky-booking, so
  change the shape in both or in neither. The classifying predicate is one line in the adapter: a `SdkServiceException`
  with a status below 500 that is not 429 is the 502 case, everything else is the 503 case.
- The `detail` never carries the store's own words. It is built from a fixed operation sentence plus one of two
  fixed suffixes, so a caller sees `Photo upload failed. The object store is unavailable.` rather than the SDK message,
  which used to put `Access Denied (Service: S3, Status Code: 403, Request ID: null)` into a 400 body. The store's
  status goes to the log instead, in `photo_storage_call_failed` with an `operation` field.
- The delete path logs and continues, deliberately. `OfferServicePrimary.removeStoredPhoto` catches
  `PhotoStorageException`, logs `photo_delete_failed` and returns, so `DELETE /owner/offers/{offerId}/photo` and
  `DELETE /owner/offers/{offerId}` both answer as they would on a reachable store. Failing them would roll back the
  cleared key or the deleted row and leave an owner unable to delete anything for the length of the outage, which is
  worse than an object nobody references: the leak is sweepable from the key prefix, the blocked delete is not
  workaroundable by the caller. Narrow that catch if a new storage exception appears, do not widen it to `Exception`,
  and do not make it swallow an upload failure, which must reach the caller.
- API docs: springdoc `webmvc` UI at `/swagger-ui/index.html`, entered at `/swagger-ui.html`, which is
  `springdoc.swagger-ui.path` here and answers a prefix-aware 302 to the page. Behind an edge both live under
  `/offer`, and the prefix reaches springdoc only because the documentation ingresses set
  `nginx.ingress.kubernetes.io/x-forwarded-prefix` and `server.forward-headers-strategy: framework` is set
  above. There is no `springdoc.swagger-ui.urls` block here and there must not be one: the dropdown entry is
  the `public` group `OpenApiAutoConfiguration` registers in sky-common, labelled from `springdoc.info.title`,
  and a local block duplicates that entry rather than replacing it. See [sky-common/AGENTS.md](../sky-common/AGENTS.md).
  The contract in
  `docs/api/openapi/sky-offer.openapi.yaml` is generated, not written. `sky.openapi-conventions` wires the
  springdoc Gradle plugin here and `build` depends on `generateOpenApiDocs`, which forks the application under
  the `local,openapi` profile pair on port 7972 and fetches the grouped document from it. Never hand-edit that
  file, and never add a default to `application-openapi.yaml` beyond what keeps the fork offline.
  The mapping declares `version = "v1"` rather than `"1"`, and that literal is not cosmetic: springdoc
  writes the version string from the mapping into the path segment the version resolver matches, so `"1"`
  published `/api/1/...` for a service that serves `/api/v1/...`. Routing is identical either way, because
  `SemanticApiVersionParser.parseVersion` calls `skipNonDigits` before it matches, so `"v1"` and `"1"` parse
  to the same version and the `addSupportedVersions("1")` and `setDefaultVersion("1")` calls in sky-common
  keep matching. Do not change it back.
  The error contract is the annotations and nothing else. The 409 on create, edit and delete, the 415 on
  every operation that takes a body, and the 503 on every operation that presigns a stored photo come from
  `@ApiConflictResponse`, `@ApiUnsupportedMediaTypeResponse` and `@ApiDependencyUnavailableResponse` in
  sky-common, with `@ApiDependencyBadGatewayResponse` beside them on the photo upload, the one operation
  that can be refused by a store that answered. The 413 stays inline here, because the 5 MB file and 6 MB
  request limits are this module's own. `GET /offers` and `POST /search` each carry `@SecurityRequirements`
  with no value, which is what makes springdoc publish `security: []` on them: the document-level bearer
  requirement from sky-common would otherwise mark two anonymous endpoints as needing a token. They still
  publish a 401, from the class-level `@ApiCommonErrorResponses`, because the resource-server filter chain
  rejects an unverifiable bearer token before either handler runs, and they publish no 403, because neither
  one checks a realm role. An anonymous endpoint here is one that needs no token, not one that ignores a bad
  token. All nine operations publish that one shared 401 description, and no method here declares a 401 of its
  own: springdoc applies the class-level `@ApiResponses` over a method-level one for the same status, so an
  inline entry for a status a class-level annotation already declares contributes nothing. `getOfferOwner`
  carried one until it was removed, and removing it left the generated document byte identical. `POST /search`
  carried a dead inline 400 for the same reason, naming the blank and length rules its body really enforces,
  and no caller ever read them: the shared 400 won and published only its generic sentence. Those rules now
  live in the operation `description`, built from `SEARCH_TERM_MAX_LENGTH` so the published text and the
  runtime message move together. A per-endpoint refinement of a shared status belongs there, because a
  response entry for that status cannot win. `OfferApiDocumentTest` pins both halves. The two tags
  are declared per method rather than on the class, because springdoc unions the class tag with the method
  tag, so a class-level `@Tag` would put every owner operation under `Offers` as well.

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit). Integration and
repository tests use Testcontainers PostgreSQL + Kafka (`@ServiceConnection`). Run from the repo root, e.g.
`./gradlew :sky-offer:test`.

`jacocoTestCoverageVerification` runs as part of `check` at 0.90 line and 0.90 branch, and the module sits at 1.00
line and 0.99 branch over the measured set. The Bruno collection under `docs/api/request/offer` is part of the photo
contract rather than a smoke test: it uploads an object carrying the canary marker `SKY-OFFER-PHOTO-CANARY-4471`,
fetches the presigned address back to prove the bytes landed, proves the object survives an edit, proves a second
upload removes the first object, and `cleanup/delete-offer.yml` refetches the address afterwards and requires a 404.
Keep those inverse assertions: a leak shows up as a 200 where the collection demands a 404.

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
- A fifth rule landed beside those four, proved the same way. No class under `adapters.inbound` may depend on a class
  under `domain.ports.outbound`, which is what stops a controller injecting a notification port and publishing its own
  event. It subsumes the repository rule above, because a repository is one driven port among several, and the
  repository rule is kept anyway for the sharper message it gives on the mistake it names. sky-notify cannot carry
  this rule, because it holds no `domain.ports.outbound` package and a selector matching nothing reads as enforcement
  while checking nothing. sky-message could carry it and does not yet, which
  `openspec/specs/architecture/spec.md` records as a gap rather than as enforcement.
