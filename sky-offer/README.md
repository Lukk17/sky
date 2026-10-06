# sky-offer

Offer management service for the Sky platform.

Port: 5552, API prefix `/api/v1`, caller identity from the JWT `email` claim.

---

### What it does

`sky-offer` is the inventory service. It owns the CRUD lifecycle of flight/hotel offers. Browsing and searching need
no token at all, and only the offer's owner can create, edit or delete one. When an offer is created, edited, or
deleted the service publishes a Kafka event so `sky-notify` can push a notification to the acting user's browser. The
domain service publishes that event, not the controller, so it is ordered against the domain's own steps and a request
the domain rejects announces nothing.

`sky-booking` calls `GET /api/v1/offers/{offerId}/owner` to resolve offer ownership during booking creation. That path
has no Ingress rule of its own, so it is reachable only from inside the cluster, and it still requires a valid token.

---

### Endpoints

Public. These two are `permitAll()` in this service's own security chain, not merely unprotected at the ingress, so they answer without a token however you reach them:

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/offers` | List all offers (paged) | |
| `POST` | `/api/v1/search` | Search offers by keyword | `String` (search term) |

Owner-only. Authenticated in this service, and additionally behind oauth2-proxy at the ingress:

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/owner/offers` | List offers owned by the authenticated user (paged) | |
| `POST` | `/api/v1/owner/offers` | Create a new offer | `OfferDTO` |
| `PUT` | `/api/v1/owner/offers` | Edit an existing offer | `OfferEditDTO` |
| `DELETE` | `/api/v1/owner/offers/{offerId}` | Delete an offer | |
| `POST` | `/api/v1/owner/offers/{offerId}/photo` | Upload or replace the offer photo (multipart) | `multipart/form-data` |
| `DELETE` | `/api/v1/owner/offers/{offerId}/photo` | Delete the stored offer photo | |

Cluster-internal, no ingress rule of its own, still authenticated:

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/offers/{offerId}/owner` | Resolve an offer's owner email, called by sky-booking | |

The edit is a partial update even though the method is `PUT`. Only `id` is required, and a field is written only
when the payload carries one, so everything you leave out keeps whatever the offer already holds, a stored null
included. `ownerEmail` in the body is ignored and the photo object key is not part of the body at all, so an edit can
neither move an offer to another owner nor touch its photo.

Nothing can be set back to null through the edit. An explicit `null` reads the same as an omission, `description` and
`comment` accept an empty string and store an empty string, and `externalPhotoUrl` rejects an empty value because it
has to be an absolute `http` or `https` URL. The one photo a caller can clear is the uploaded object, with `DELETE` on
the photo path.

Photo upload accepts a `multipart/form-data` request with a single part named `file`. The uploaded bytes are stored in the S3-compatible object store under the key `offers/{offerId}/{uuid}-{filename}`, and that key is persisted in the `photo_object_key` column of the `offer` table. There is no separate photo table. The response is the updated `OfferDTO` that includes a `photoUrl` field containing a time-limited presigned GET URL (default 15 minutes, configurable via `S3_PRESIGN_TTL`).

#### The server owns the key, a client refers to the photo by the offer id

`photo_object_key` is written by the photo upload and cleared by the photo delete, and by nothing else. It is absent from `OfferDTO` and from `OfferEditDTO`, so no request body can set it, change it, or read it back. A client addresses the photo through the offer: `POST` and `DELETE` on `/api/v1/owner/offers/{offerId}/photo`, and `photoUrl` on any response that carries an offer.

A second upload replaces the first and deletes the object it replaced, so the bucket never holds a photo the offer no longer points at. `DELETE` on the photo path is the only way to clear a photo, deleting the offer deletes its object with it, and an edit cannot touch either.

`external_photo_url` is the other half of the split, and it is the one field a client may write. It holds the address of an image hosted somewhere else, for an offer whose owner never uploaded anything, which is what the demo seed rows use. Bean Validation requires an absolute `http` or `https` URL, so its accepted values and the `offers/{offerId}/...` key namespace do not overlap, and the storage adapter is never handed a value that came from a request.

All endpoints that return `OfferDTO` (list, search, create, edit, photo upload) populate `photoUrl`: the presigned URL of the uploaded object when the offer has one, `externalPhotoUrl` otherwise, and null when it has neither.

Pagination. `GET /api/v1/offers` and `GET /api/v1/owner/offers` both return a Spring Data `Page<OfferDTO>` envelope. Use the `page` and `size` query parameters to control pagination (default: page 0, size 20):

```text
GET /api/v1/offers?page=0&size=10
```

The response body has the following structure:

```json
{
  "content": [ { ...OfferDTO... } ],
  "totalElements": 42,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

`OfferDTO` (create):

```json
{
  "hotelName": "Grand Hotel",
  "description": "Sea view double room",
  "comment": "",
  "price": "299.00",
  "roomCapacity": "2",
  "city": "Barcelona",
  "country": "Spain",
  "externalPhotoUrl": "https://images.example.com/grand-hotel.jpg"
}
```

The caller's identity comes from the `email` claim of the validated bearer token, read through
`SecurityUtils.currentUserEmail()` in `sky-common`. The service does not read the `x-auth-request-email` header that
`oauth2-proxy` forwards.

Swagger UI: `http://localhost:5552/swagger-ui/index.html` (local) or
`https://skycloud.luksarna.com/offer/swagger-ui/index.html` (cluster).

---

### What a lost race answers

The three write operations append an event to the offer's event log: `POST /api/v1/owner/offers`,
`PUT /api/v1/owner/offers` and `DELETE /api/v1/owner/offers/{offerId}`. When another writer advances the sequence
number while one of those is in flight, and all twenty append attempts lose, the request answers 409 Conflict with an
RFC 9457 problem detail. It had no mapping at all before this, so it fell through to a bare 500 with Spring Boot's
default error body.

| Condition | Exception | Status |
|---|---|---|
| The offer event sequence moved while the write was in flight | `EventSequenceConflictException` | 409 |

No `Retry-After` is sent, because the caller has to re-read the offer before a retry can succeed rather than simply
wait. The `detail` is a fixed sentence saying exactly that, rather than the internal message, so nothing about the
event log reaches the caller.

---

### What a failing object store answers

A photo lives in an S3-compatible object store, and that store is a dependency this service does not control. When it
fails, the answer says so: 503 Service Unavailable with `Retry-After: 10` when the store could not be used at all, and
502 Bad Gateway when it answered and answered unusably. Both carry an RFC 9457 problem detail. Every one of these used
to answer 400 Bad Request, which told the caller its request was malformed when the request was fine, and told every
client not to retry at the exact moment retrying was the right move.

| Condition | Exception | Status |
|---|---|---|
| The store refuses the connection | `PhotoStorageUnavailableException` | 503 with `Retry-After: 10` |
| The connection times out | `PhotoStorageUnavailableException` | 503 with `Retry-After: 10` |
| The read times out | `PhotoStorageUnavailableException` | 503 with `Retry-After: 10` |
| The store answers 5xx | `PhotoStorageUnavailableException` | 503 with `Retry-After: 10` |
| The store answers 429 | `PhotoStorageUnavailableException` | 503 with `Retry-After: 10` |
| A stored photo address cannot be signed | `PhotoStorageUnavailableException` | 503 with `Retry-After: 10` |
| The store answers 401 or 403 on wrong credentials | `PhotoStorageBadResponseException` | 502 |
| The bucket does not exist | `PhotoStorageBadResponseException` | 502 |
| The upload is not a JPEG, PNG, GIF or WebP | `OfferException` | 400 |
| The `file` part is missing or empty | `OfferException` | 400 |
| The upload is over 5 MB, or the request over 6 MB | servlet size cap | 413 |

Neither storage exception extends `OfferException`, and that is the point: `OfferException` maps to 400, so a storage
failure that inherited from it would drift back to 400 the next time somebody adds a handler. The two types sit outside
that hierarchy, exactly as `OfferServiceUnavailableException` and `OfferServiceBadResponseException` do in sky-booking.

The `detail` of either is one of a fixed set of sentences. Nothing from the store's own answer reaches it, so the
earlier behaviour of echoing `Access Denied (Service: S3, Status Code: 403, Request ID: null)` into a response body is
gone too.

Deleting is the one path that does not fail. `DELETE` on the photo path, and deleting a whole offer, clear the row and
then remove the object, and a storage failure during that removal is logged as `photo_delete_failed` and swallowed, so
both answer as they would have on a reachable store. Failing instead would roll the cleared key back and leave an owner
unable to delete their own photo or their own offer for as long as the store is down, which is a worse outcome than an
object nobody references. The leaked object is reachable from the key prefix and can be swept later, an undeletable
offer cannot be worked around by the caller at all.

---

### Architecture

Uses hexagonal (ports-and-adapters):

- `domain/model`, `domain/exception`: the core model and its exceptions.
- `domain/ports/inbound`: `OfferService`, the driving port the controller calls.
- `domain/ports/outbound`: `OfferRepository`, `EventSourceRepository`, `OfferNotificationService`, and `PhotoStorage`, the driven ports the adapters implement.
- `domain/service`: `OfferServicePrimary` and the event-source services.
- `adapters/inbound/api`: `OfferApiController`, which serves the public, owner, and cluster-internal paths including photo upload and photo delete.
- `adapters/outbound/notification`: the Kafka producer. It wraps events in `KafkaPayloadModel` from `sky-common`, mints the envelope timestamp, serialises the payload and writes the delete sentence, so the driven port names the event and the domain never touches the wire shape.
- `adapters/outbound/storage`: `S3PhotoStorage`, the AWS SDK v2 adapter implementing `PhotoStorage` with `S3Client` and `S3Presigner`. The same adapter serves the cluster and a laptop, both of which run floci, and would serve a managed S3 unchanged, because the only contract is the S3 API.
- `adapters/dto`: the `OfferDTO` and `OfferEditDTO` wire types. `OfferDTO` carries the derived `photoUrl`, neither carries the object key, and `OfferEditDTO.applyTo` merges a partial update onto the stored entity without touching the key or the owner.
- `config`, `config/kafka`, `config/propertyBind`: Spring, Kafka, and S3 wiring.

Plain Spring MVC stack (`spring-boot-starter-web`). No WebFlux, and the same is now true of every service: `sky-gateway` is the only module on the reactive stack.

The producer is configured for durability rather than for throughput, and all five delivery-guarantee properties are written down rather than inherited: `acks=all`, `enable.idempotence=true`, `retries=2147483647`, `delivery.timeout.ms=120000` and `max.in.flight.requests.per.connection=5`. Three of them constrain each other. The in-flight value is 5 because that is the most an idempotent producer is allowed, and kafka-clients 4.1.2 refuses to build a producer above it. The delivery timeout has to be at least `linger.ms + request.timeout.ms`, which is 30005 with the defaults this service leaves in place, and the client refuses an explicit value below that sum. `retries` is honestly a pin rather than a guarantee: once a delivery timeout is set, that timeout is what bounds retrying, and any non-zero retry count behaves the same. All three of those values equal the current client default, which is the reason they are written down at all: a default that moves between client versions must not be able to change the durability of a write silently. It is set in [src/main/resources/application.yaml](src/main/resources/application.yaml) and asserted by `KafkaProducerDeliveryGuaranteeTest` against the configuration the booted context resolves, not against the YAML file. sky-booking and sky-notify carry the same five values.

Schema versioning via Flyway. Migrations in [src/main/resources/db/migration/](src/main/resources/db/migration/), against the shared `sky` database.

---

### Key environment variables

| Variable | Default | Notes |
|---|---|---|
| `OFFER_PORT` | `5552` | Service port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://host.docker.internal:5432/sky` | JDBC URL |
| `POSTGRES_USER` | none, required | Database username. Unset, the service refuses to start and names the variable |
| `POSTGRES_PASSWORD` | none, required | Database password. Unset, the service refuses to start and names the variable |
| `KAFKA_ADDRESS` | `kafka-service` | Kafka host |
| `KAFKA_PORT` | `9092` | Kafka port |
| `OAUTH2_ISSUER_URI` | `https://keycloak.test:9443/realms/sky` | OIDC issuer used to validate bearer tokens |
| `OAUTH2_AUDIENCE` | unset | Set to `sky-backend` to enforce the audience claim |

Photo storage against the object store. The defaults below are the committed local-development values and need no export, with one exception: `S3_ACCESS_KEY` and `S3_SECRET_KEY` have no default on the default profile. They are defaulted only in `application-local.yaml`, so a `local` run needs nothing exported and any other profile has to supply them or the service refuses to start.

| Variable | Default | Notes |
|---|---|---|
| `S3_ENDPOINT` | `http://localhost:9070` | Where this service sends its own S3 calls: upload, delete, bucket creation |
| `S3_PRESIGN_ENDPOINT` | unset | What a presigned URL names for the client that fetches it. Falls back to `S3_ENDPOINT` when empty |
| `S3_REGION` | `us-east-1` | AWS region, which a local emulator ignores but the SDK requires |
| `S3_BUCKET` | `sky-offers` | Bucket name, created on startup if missing |
| `S3_ACCESS_KEY` | `root` under the `local` profile, otherwise required | S3 access key. An unset value on any other profile fails startup |
| `S3_SECRET_KEY` | `localdev` under the `local` profile, otherwise required | S3 secret key. An unset value on any other profile fails startup |
| `S3_PATH_STYLE` | `true` | Force path-style URLs, required by floci and by MinIO |
| `S3_PRESIGN_TTL` | `PT15M` | Presigned URL lifetime (ISO-8601 duration) |

#### The two endpoints are not one endpoint

`S3_ENDPOINT` and `S3_PRESIGN_ENDPOINT` answer different questions and a single value cannot answer both unless one hostname happens to resolve on both sides of the boundary.

`S3_ENDPOINT` is the address this service dials. In a cluster the chart sets it to `http://floci-service:4566`, which resolves through Kubernetes DNS and nowhere else.

`S3_PRESIGN_ENDPOINT` is the address written into the presigned URL handed back as `OfferDTO.photoUrl`. A browser or a test runner outside the cluster has to resolve and reach it. The local overlay sets it to `http://s3.localhost:5777`, the floci ingress on the same host port the rest of the stack answers on. Left empty, the presigner falls back to `S3_ENDPOINT`, which is right under docker-compose, where `s3.localhost:9070` resolves inside the container and on the host alike, and wrong in a cluster, where the internal service name resolves for no client.

A presigned URL is signed against the host it names, and the port is part of that host, so the store must receive the same `Host` header the signature was computed over. Routing through nginx preserves it: the ingress forwards the client `Host` verbatim. Change the port a client dials without changing `S3_PRESIGN_ENDPOINT` and a store that verifies signatures answers `SignatureDoesNotMatch`.

Credentials come from the chart either way: it reads them from the `s3-access-key` and `s3-secret-key` entries of the `sky-secrets` Secret, see [../config/k8s/helm/service/sky-offer/values.yaml](../config/k8s/helm/service/sky-offer/values.yaml). floci ignores them, the AWS SDK will not build a client without them.

The service boots even when the object store is unreachable. [src/main/java/com/lukk/sky/offer/config/S3Config.java](src/main/java/com/lukk/sky/offer/config/S3Config.java) runs the bucket check on `ApplicationReadyEvent`, creates the bucket when it is missing, and downgrades a connection failure to a warning. The photo endpoints then answer 503 with `Retry-After: 10` at request time until the store comes back, and any read that has to sign a stored photo address does the same.

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-offer:test
```

Integration and repository tests use Testcontainers `postgres:17-alpine` and Kafka containers, wired through `@ServiceConnection`. Docker must be running: there is no in-memory fallback, because H2 is gone from this module and from the version catalogue.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Platform overview, modules, build, ports |
| [AGENTS.md](AGENTS.md) | Module-local agent and coding conventions |
| [src/main/resources/db/migration/](src/main/resources/db/migration/) | Flyway SQL migrations owned by this service |
| [../config/local-dev/local_README.md](../config/local-dev/local_README.md) | Running the platform locally |
| [../docs/api/README.md](../docs/api/README.md) | Bruno collection and OpenAPI specs |
