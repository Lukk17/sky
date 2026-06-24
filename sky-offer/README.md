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

- `domain/model`, `domain/ports`, `domain/exception`: core domain. `OfferService` is the primary port.
- `adapters/api`: `OfferApiController` (public + owner REST) and `OfferInternalController` (intra-cluster only).
- `adapters/notification`: outbound Kafka producer.
- `adapters/dto`: `OfferDTO`, `OfferEditDTO` wire types.
- `config/kafka`, `config/propertyBind`: Spring and Kafka wiring.

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
