# sky-booking

*Booking lifecycle service for the Sky platform.*

Port: **5555** | API prefix: `/api/v1` | Identity header: `x-auth-request-email`

---

### What it does

`sky-booking` manages bookings placed by authenticated users against offers. It exposes REST endpoints for listing,
creating, and deleting bookings. On each mutation it calls `sky-offer` over internal REST to resolve offer ownership,
persists the booking to MySQL, and publishes a Kafka event so `sky-notify` can push a real-time update to the user's
browser.

---

### Endpoints

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/user/bookings` | List all bookings for the authenticated user (paged) | |
| `POST` | `/api/v1/bookings` | Create a new booking | `BookingPayload` |
| `DELETE` | `/api/v1/bookings/{bookingId}` | Delete a booking owned by the authenticated user | |

**Pagination** — `GET /api/v1/user/bookings` returns a Spring Data `Page<BookingDTO>` envelope. Use the `page` and `size` query parameters to control pagination (default: page 0, size 20):

```
GET /api/v1/user/bookings?page=0&size=10
```

The response body has the following structure:

```json
{
  "content": [ { ...BookingDTO... } ],
  "totalElements": 15,
  "totalPages": 2,
  "size": 10,
  "number": 0
}
```

`BookingPayload`:

```json
{
  "offerId": "123",
  "dateToBook": "2024-09-15"
}
```

Date format: `YYYY-MM-DD`.

User identity is read from the `x-auth-request-email` header, which the `oauth2-proxy` ingress sets after validating
the session. The service never reads a token directly.

---

### Architecture

Uses the hexagonal (ports-and-adapters) pattern:

- `domain/model`, `domain/ports`, `domain/exception`: core domain. `BookingService` is the primary port.
- `adapters/api`: REST controller (`BookingController`). Reads identity from request headers.
- `adapters/notification`: outbound Kafka producer that wraps events in `KafkaPayloadModel` from `sky-common`.
- `config/kafka`, `config/propertyBind`: Spring and Kafka wiring.

This service uses WebFlux (`WebClient`) for the outbound call to `sky-offer`'s internal endpoint. The MVC surface
(REST controller) is still blocking Spring Web. Do not swap `WebClient` to `RestClient` without a deliberate decision.

Schema versioning is handled by Flyway. Migration scripts live in
[src/main/resources/db/migration/](src/main/resources/db/migration/).

---

### Key environment variables

| Variable | Default | Notes |
|---|---|---|
| `BOOKING_PORT` | `5555` | Service port |
| `MYSQL_USER` | (required) | DB username |
| `MYSQL_PASS` | (required) | DB password |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://host.docker.internal:3306/sky` | JDBC URL |
| `KAFKA_ADDRESS` | `kafka-service` | Kafka host |
| `KAFKA_PORT` | `9092` | Kafka port |
| `OFFER_SERVICE_HOSTNAME` | `sky-offer-service` | Internal hostname for sky-offer |

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-booking:test
```

Integration tests use Testcontainers MySQL and Kafka. Docker must be running. H2 is used for unit-level repository
tests. ArchUnit enforces hexagonal layer direction; adapters must not import domain internals.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Root README: full platform overview, build, deployment |
| [AGENTS.md](AGENTS.md) | Module-local agent/coding conventions |
| [src/main/resources/db/migration/](src/main/resources/db/migration/) | Flyway SQL migrations |
