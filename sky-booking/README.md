# sky-booking

Booking lifecycle service for the Sky platform.

Port: 5555, API prefix `/api/v1`, caller identity from the JWT `email` claim.

---

### What it does

`sky-booking` manages bookings placed by authenticated users against offers. It exposes REST endpoints for listing,
creating, and deleting bookings. On each mutation it calls `sky-offer` over internal REST to resolve offer ownership,
persists the booking to PostgreSQL, and publishes a Kafka event so `sky-notify` can push a real-time update to the
user's browser.

---

### Endpoints

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/user/bookings` | List all bookings for the authenticated user (paged) | |
| `POST` | `/api/v1/bookings` | Create a new booking | `BookingPayload` |
| `DELETE` | `/api/v1/bookings/{bookingId}` | Delete a booking owned by the authenticated user | |

Pagination. `GET /api/v1/user/bookings` returns a Spring Data `Page<BookingDTO>` envelope. Use the `page` and `size` query parameters to control pagination (default: page 0, size 20):

```text
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
  "offerId": "3f7a1c88-5d24-4b6e-9c0f-1a2b3c4d5e6f",
  "dateToBook": "2027-08-15"
}
```

Date format: `YYYY-MM-DD`.

The caller's identity comes from the `email` claim of the validated bearer token, read through
`SecurityUtils.currentUserEmail()` in `sky-common`. The service does not read the `x-auth-request-email` header that
`oauth2-proxy` forwards.

---

### Architecture

Uses the hexagonal (ports-and-adapters) pattern:

- `domain/model`, `domain/exception`: the core model and its exceptions.
- `domain/ports/inbound`: `BookingService`, the driving port the controller calls.
- `domain/ports/outbound`: `BookingRepository`, `EventSourceRepository`, `BookingNotificationService`, `RestClient`, and `RequestUriStrategy`, the driven ports the adapters implement.
- `domain/service`: `BookingServicePrimary`, `BookingPersister`, and the event-source services.
- `adapters/inbound/api`: the REST controller. Reads the caller from the validated JWT through `SecurityUtils.currentUserEmail()`, never from a request header.
- `adapters/outbound/rest`: the client, caller, and URI strategy for the call to sky-offer.
- `adapters/outbound/notification`: the Kafka producer that wraps events in `KafkaPayloadModel` from `sky-common`.
- `adapters/dto`: wire DTOs.
- `config`, `config/kafka`, `config/propertyBind`: Spring and Kafka wiring.

The producer is configured for durability rather than for throughput, and all five delivery-guarantee properties are written down rather than inherited: `acks=all`, `enable.idempotence=true`, `retries=2147483647`, `delivery.timeout.ms=120000` and `max.in.flight.requests.per.connection=5`. Three of them constrain each other. The in-flight value is 5 because that is the most an idempotent producer is allowed, and kafka-clients 4.1.2 refuses to build a producer above it. The delivery timeout has to be at least `linger.ms + request.timeout.ms`, which is 30005 with the defaults this service leaves in place, and the client refuses an explicit value below that sum. `retries` is honestly a pin rather than a guarantee: once a delivery timeout is set, that timeout is what bounds retrying, and any non-zero retry count behaves the same. All three of those values equal the current client default, which is the reason they are written down at all: a default that moves between client versions must not be able to change the durability of a write silently. It is set in [src/main/resources/application.yaml](src/main/resources/application.yaml) and asserted by `KafkaProducerDeliveryGuaranteeTest` against the configuration the booted context resolves, not against the YAML file. sky-offer and sky-notify carry the same five values.

The stack is plain Spring MVC throughout. The outbound call to `sky-offer` goes through Spring's synchronous
`RestClient`, configured in [src/main/java/com/lukk/sky/booking/config/RestClientConfig.java](src/main/java/com/lukk/sky/booking/config/RestClientConfig.java),
which also installs the correlation-id interceptor from `sky-common`. Resilience4j wraps that call with a retry and a
circuit breaker, both on the `offerService` instance and both configured under `resilience4j` in
[src/main/resources/application.yaml](src/main/resources/application.yaml).

Those annotations sit on `OfferServiceCaller`, a separate bean from `OfferRestClient` which calls it. The split is
deliberate rather than accidental structure: Resilience4j works through Spring AOP proxies, so annotating a method and
then calling it as `this.method()` from the same class bypasses the proxy and silently does nothing. `OfferRestClient`
is left with one job, turning an exhausted retry into an `OfferServiceUnavailableException` so the domain layer never
sees a raw HTTP-client exception. Do not merge the two classes back together.

The circuit breaker counts transport failures only. `record-exceptions` on the `offerService` instance lists
`ResourceAccessException` and `IOException`, so a run of requests for offers that do not exist cannot fill the ten call
window and open the breaker.

Schema versioning is handled by Flyway. Migration scripts live in
[src/main/resources/db/migration/](src/main/resources/db/migration/).

---

### What the offer lookup answers

A booking that cannot resolve the offer owner never answers 400, because the caller's request was not the problem.
Each outcome has its own exception type and its own status:

| Condition on the call to sky-offer | Exception | Status |
|---|---|---|
| Connection refused | `OfferServiceUnavailableException` | 503 with `Retry-After: 10` |
| Connect timeout, 2 seconds | `OfferServiceUnavailableException` | 503 with `Retry-After: 10` |
| Read timeout, 3 seconds | `OfferServiceUnavailableException` | 503 with `Retry-After: 10` |
| 5xx, after the three attempts are exhausted | `OfferServiceUnavailableException` | 503 with `Retry-After: 10` |
| Circuit breaker open | `OfferServiceUnavailableException` | 503 with `Retry-After: 10` |
| 404, the offer does not exist | `OfferNotFoundException` | 404 |
| Any other client-error status | `OfferServiceBadResponseException` | 502 |

400 is reserved for a request that really is wrong. On this endpoint the only domain case left on it is a date in the
past.

### What a lost race answers

Two conditions mean the request was fine and only arrived second. Both answer 409, and neither sends `Retry-After`,
because the caller has to look at the current state rather than simply wait:

| Condition | Exception | Status |
|---|---|---|
| The offer already has a booking on that date | `BookingDateAlreadyBookedException` | 409 |
| The booking event sequence moved while the write was in flight | `EventSequenceConflictException` | 409 |

The already-booked case answered 400 until now, which told a client its request was malformed when the truthful answer
was to pick another date. The sequence conflict had no mapping at all and fell through to a bare 500 with Spring Boot's
default error body. Its `detail` is a fixed sentence that tells the caller to re-read the booking before retrying, since
the conflict means another writer advanced the sequence and repeating the same write would lose the same race.

---

### Key environment variables

| Variable | Default | Notes |
|---|---|---|
| `BOOKING_PORT` | `5555` | Service port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://host.docker.internal:5432/sky` | JDBC URL |
| `POSTGRES_USER` | none, required | Database username |
| `POSTGRES_PASSWORD` | none, required | Database password |
| `KAFKA_ADDRESS` | `kafka-service` | Kafka host |
| `KAFKA_PORT` | `9092` | Kafka port |
| `OFFER_SERVICE_HOSTNAME` | `sky-offer-service` | Internal hostname for sky-offer |
| `OFFER_SERVICE_HOST_PORT` | empty | Port appended to the hostname, left empty in the cluster where the service name resolves on 80 |
| `OAUTH2_ISSUER_URI` | `https://keycloak.test:9443/realms/sky` | OIDC issuer used to validate bearer tokens |
| `OAUTH2_AUDIENCE` | unset | Set to `sky-backend` to enforce the audience claim |

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-booking:test
```

Integration and repository tests use Testcontainers `postgres:17-alpine` and Kafka containers, wired through
`@ServiceConnection`. Docker must be running: there is no in-memory fallback, because H2 has been removed from this
module and from the version catalogue. ArchUnit enforces the hexagonal layer direction (adapters depend on
`domain/ports`, never on `domain/service`) and asserts the whole module stays off the reactive stack, so a stray
Reactor or WebFlux import fails the build.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Platform overview, modules, build, ports |
| [AGENTS.md](AGENTS.md) | Module-local agent and coding conventions |
| [src/main/resources/db/migration/](src/main/resources/db/migration/) | Flyway SQL migrations owned by this service |
| [../config/local-dev/local_README.md](../config/local-dev/local_README.md) | Running the platform locally |
| [../docs/api/README.md](../docs/api/README.md) | Bruno collection and OpenAPI specs |
