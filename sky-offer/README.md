# sky-offer

*Offer management service for the Sky platform.*

Port: **5552** | API prefix: `/api/v1` | Identity header: `x-auth-request-email`

---

### What it does

`sky-offer` is the inventory service. It owns the CRUD lifecycle of flight/hotel offers. Any authenticated user can
browse or search offers; only the offer's owner can edit or delete it. When an offer is created, edited, or deleted
the service publishes a Kafka event so `sky-notify` can push a notification to the acting user's browser.

`sky-booking` calls `sky-offer`'s internal endpoint (`/api/internal/owner/offer`) to resolve offer ownership during
booking creation. That endpoint is not exposed through the public ingress.

---

### Endpoints

**Public (no authentication required at the ingress level):**

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/offers` | List all offers (paged) | |
| `POST` | `/api/v1/search` | Search offers by keyword | `String` (search term) |

**Owner-only (require `oauth2-proxy` authentication):**

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/owner/offers` | List offers owned by the authenticated user (paged) | |
| `POST` | `/api/v1/owner/offers` | Create a new offer | `OfferDTO` |
| `PUT` | `/api/v1/owner/offers` | Edit an existing offer | `OfferEditDTO` |
| `DELETE` | `/api/v1/owner/offers/{offerId}` | Delete an offer | |
| `POST` | `/api/v1/owner/offers/{offerId}/photo` | Upload or replace the offer photo (multipart) | `multipart/form-data` |

Photo upload accepts a `multipart/form-data` request with a single part named `file`. The uploaded bytes are stored in MinIO (S3-compatible object storage) under the key `offers/{uuid}-{filename}`. The stored object key is persisted in `Offer.photoPath`. The response is the updated `OfferDTO` that includes a `photoUrl` field containing a time-limited presigned GET URL (default 15 minutes; configurable via `S3_PRESIGN_TTL`).

All other endpoints that return `OfferDTO` (list, search, create, edit) also populate `photoUrl` automatically for any offer that has a non-blank `photoPath`.

**Pagination** — `GET /api/v1/offers` and `GET /api/v1/owner/offers` both return a Spring Data `Page<OfferDTO>` envelope. Use the `page` and `size` query parameters to control pagination (default: page 0, size 20):

```
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
  "photoPath": ""
}
```

User identity is read from the `x-auth-request-email` header injected by `oauth2-proxy`.

Swagger UI: `http://localhost:5552/swagger-ui/index.html` (local) or
`https://skycloud.luksarna.com/offer/swagger-ui/index.html` (cluster).

---

### Architecture

Uses hexagonal (ports-and-adapters):

- `domain/model`, `domain/ports`, `domain/exception`: core domain. `OfferService` is the primary port. `PhotoStorage` (in `domain/ports/storage`) is the outbound port for binary photo storage.
- `adapters/api`: `OfferApiController` (public + owner REST, including photo upload) and `OfferInternalController` (intra-cluster only).
- `adapters/notification`: outbound Kafka producer.
- `adapters/storage`: `S3PhotoStorage` — AWS SDK v2 adapter implementing `PhotoStorage` using S3Client and S3Presigner.
- `adapters/dto`: `OfferDTO` (now includes `photoUrl` presigned-URL field), `OfferEditDTO` wire types.
- `config/kafka`, `config/propertyBind`: Spring, Kafka, and S3 wiring.

Plain Spring MVC stack (`spring-boot-starter-web`); no WebFlux here, unlike `sky-booking`.

Schema versioning via Flyway. Migrations in [src/main/resources/db/migration/](src/main/resources/db/migration/).

---

### Key environment variables

| Variable | Default | Notes |
|---|---|---|
| `OFFER_PORT` | `5552` | Service port |
| `MYSQL_USER` | (required) | DB username |
| `MYSQL_PASS` | (required) | DB password |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://host.docker.internal:3306/sky` | JDBC URL |
| `KAFKA_ADDRESS` | `kafka-service` | Kafka host |
| `KAFKA_PORT` | `9092` | Kafka port |

**Photo storage (MinIO / S3):**

| Variable | Default | Notes |
|---|---|---|
| `S3_ENDPOINT` | `http://localhost:9000` | MinIO endpoint URL |
| `S3_REGION` | `us-east-1` | AWS region (MinIO ignores value but requires it) |
| `S3_BUCKET` | `sky-offers` | Bucket name (auto-created on startup if missing) |
| `S3_ACCESS_KEY` | `minioadmin` | S3 / MinIO access key |
| `S3_SECRET_KEY` | `minioadmin` | S3 / MinIO secret key |
| `S3_PATH_STYLE` | `true` | Force path-style URLs (required for MinIO) |
| `S3_PRESIGN_TTL` | `PT15M` | Presigned URL lifetime (ISO-8601 duration) |

The service is resilient to MinIO being unavailable at startup: the bucket-existence check is wrapped in a try/catch that logs a warning and lets the application boot normally. Photo-related endpoints will return errors if MinIO is unreachable at request time.

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-offer:test
```

Integration tests use Testcontainers MySQL and Kafka. Docker must be running.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Root README: full platform overview, build, deployment |
| [AGENTS.md](AGENTS.md) | Module-local agent/coding conventions |
| [src/main/resources/db/migration/](src/main/resources/db/migration/) | Flyway SQL migrations |
