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
- Photo keys are namespaced as `offers/{offerId}/{uuid}-{filename}`, and `S3PhotoStorage.delete` refuses any
  key outside the prefix for the offer it was handed. That closes a cross-tenant deletion hole, so do not relax it.
- Persistence: PostgreSQL (`org.postgresql:postgresql`, runtime, with `flyway-database-postgresql`) against the
  shared `sky` database, versioned with Flyway (`src/main/resources/db/migration/`, now V1 to V4). Tests run
  against a Testcontainers `postgres:17-alpine` container, never an in-memory database. H2 is gone from this module,
  from the shared test stack, and from the version catalogue. V3 and V4 are one change in two files because DDL and
  DML never share a migration: V3 adds `photo_object_key`, renames `photo_path` to `external_photo_url` and widens
  it, and V4 classifies the rows written before the split. `PhotoColumnMigrationTest` drives both against a
  Testcontainers Postgres, with rows inserted at V2, and pins what each kind of old value becomes.
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
- Messaging: produces/consumes Kafka events via `spring-kafka`, serialised with the Spring-managed Jackson 3
  `ObjectMapper` (`tools.jackson.databind.ObjectMapper`). Gson is gone from this module and from its build file.
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
- API docs: springdoc `webmvc` UI at `/swagger-ui/index.html`.

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
