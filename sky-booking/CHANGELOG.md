# Changelog: sky-booking

All notable changes to this module are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to
[Semantic Versioning](https://semver.org/). Newest entry on top. The topmost `## [x.y.z]`
version is the current one. The release workflow reads it for the image tag and to guard
against re-publishing an already-released version, so keep it at the top and bump it before
every release. The `version` in `build.gradle.kts` is a cosmetic label the release workflow
does not read. If the two disagree, this file wins for release purposes.

## [2.0.0]

Major. Four contracts this service publishes have changed shape, and a client built against
1.x will not work against it. Read the Changed section before upgrading.

### Changed
- Booking and offer identifiers are UUIDs, not numbers. `Booking.id` was a database-generated
  `Long` and `Booking.offerId` was a `String` holding a number. Both are now `UUID`, in the
  entity, in `BookingDTO`, in `BookingPayload`, and in the `DELETE /bookings/{bookingId}`
  path variable. This breaks the Sky-View frontend on its own: a client that sends
  `{"offerId": 7}` now gets a 400, and a client that expects a numeric `id` back in the
  response gets a 36 character string. There is no migration from the old numeric ids,
  because the underlying database changed with them.
- The database is PostgreSQL, not MySQL. The driver is `org.postgresql.Driver`, the schema is
  owned by Flyway (`V1__init.sql` creating `booking` and `booking_event`, `V2` adding the
  indexes and the event sequence constraint), and `hibernate.ddl-auto` is `validate` so the
  application refuses to start against a schema its entities do not match. The Flyway history
  table is `flyway_schema_history_booking`, so the three services sharing the `sky` schema
  each version their own tables instead of fighting over one history table.
- Authentication is a Keycloak-issued JWT this service validates itself, not a header the
  edge was trusted to set. 1.x read the caller identity out of a request header map and threw
  if no known header was present, which meant anything that could reach the port could claim
  any identity. The service is now an OAuth2 resource server: the bearer token is validated
  against `OAUTH2_ISSUER_URI`, its audience is checked, the realm roles become authorities,
  and every endpoint carries `@IsUser`. A request with no token gets 401 with an empty body
  rather than a 402 with an explanation.
- Listing endpoints return a page, not an array. `GET /user/bookings` returned a bare JSON
  array of every booking the user had. It returns a Spring `Page` object now, default size 20,
  so a caller reads the items out of `content` and the total out of `totalElements`. Accepts
  `page`, `size` and `sort` query parameters.
- Error responses are RFC 9457 problem documents, not free text. `POST /bookings` used to
  catch everything and answer 400 with the exception message as the raw body. A missing offer
  is now 404, a validation failure is 400 with a `field-errors` object naming each rejected
  field, and an unauthorized caller is 401. Anything parsing the old message strings has to
  be rewritten.
- `DELETE /bookings/{bookingId}` answers 204 with no body. It answered 200 with a JSON-encoded
  confirmation string. A client reading that string has nothing to read now, and a client
  checking for 200 has to accept 204.
- `POST /bookings` is a synchronous transaction rather than a reactive pipeline. The controller
  returned `Mono<ResponseEntity>` on WebFlux and the booking was committed outside any
  transaction boundary the caller could rely on. The service runs on Spring MVC with virtual
  threads now, the write is `@Transactional`, and the event append runs in its own
  `REQUIRES_NEW` transaction so an event-store failure cannot roll back a booking that the
  caller was already told succeeded. The module no longer depends on WebFlux.
- The outbound call to sky-offer goes to `GET /api/v1/offers/{offerId}/owner` and carries the
  caller's bearer token. 1.x called `/api/v1/owner/offer/{offerId}` with no credentials, which
  only worked because sky-offer trusted whatever reached it. The client is a Spring
  `RestClient` rather than a `WebClient`.
- JSON is Jackson 3 (`tools.jackson`), not Gson. The controller used a `new Gson()` per
  request to serialise notification payloads. Field order and null handling in the Kafka
  payload can differ from 1.x output as a result.
- Spring Boot 4.0.7 on Java 25, built by Gradle 9.6.1 as a module of the composite build.
  The per-module wrapper, `settings.gradle.kts` and Gradle directory are gone: build from the
  repository root with `./gradlew :sky-booking:test`. Dependency versions come from
  `gradle/libs.versions.toml` and shared build logic from the `buildSrc` convention plugins.

### Added
- Resilience on the sky-offer lookup: three attempts with exponential backoff and jitter, a
  circuit breaker over a ten call window that opens at a 50 percent failure rate, and a
  fallback. A slow or dead sky-offer previously hung the booking request until the socket gave
  up. Retry deliberately ignores `BookingException`, so a genuine business rejection is not
  retried three times.
- Hexagonal layout with the dependency direction enforced by ArchUnit. `domain/ports/inbound`
  holds what controllers call, `domain/ports/outbound` holds what adapters implement, and
  `domain/service` holds the implementations. An adapter importing from `domain/service`
  fails the build rather than passing review.
- Testcontainers PostgreSQL and Kafka integration tests through `@ServiceConnection`, an
  ArchUnit layering suite, and a resilience test that proves the retry and the circuit breaker
  behave as configured.
- Connection pool sizing (Hikari, maximum 20, minimum idle 5, five second connection timeout)
  and a durable Kafka producer (`acks=all`, idempotence enabled). 1.x ran pool defaults and a
  fire and forget producer, so a broker restart dropped notifications silently.
- A local profile that starts without Keycloak, a per-service startup banner, and a structured
  startup log line naming the deployment it thinks it is in.

### Removed
- The `GET /` and `GET /home` probe endpoints, which returned a configured greeting string and
  published a Kafka notification as a side effect of being scraped. Health now lives at
  `/actuator/health`, with separate liveness and readiness probes.
- The MySQL driver, the reactive `WebClient` stack, and the module's own copies of
  `KafkaPayloadModel`, `SwaggerConfig`, `WebClientConfig` and the three `propertyBind`
  classes, all of which now come from sky-common.

### Fixed
- An unreachable sky-offer was reported to the caller as 400 Bad Request. The outbound client
  turned an exhausted retry into a `BookingException` and the advice maps that to 400, so a
  perfectly well formed booking request was blamed for a dependency being down, and every
  client was told not to retry something that was only transient. A dependency outage is now
  its own domain exception, `OfferServiceUnavailableException`, which the advice answers with
  503 and `Retry-After: 10`, as an RFC 9457 problem document like every other error here. It
  covers a refused connection, a connect timeout, a read timeout, a 5xx after the three
  attempts are exhausted, and an open circuit breaker. A client that branched on 400 to mean a
  bad payload now sees 503 when sky-offer is down.
- A status from sky-offer other than 404 was also reported as 400. That case is now
  `OfferServiceBadResponseException`, answered with 502 Bad Gateway, because the caller's
  request was well formed and retrying it unchanged will not help. `BookingException` keeps its
  400 and now means only what it says: the request itself was wrong, which on this endpoint
  means a date in the past.
- Booking a date the offer already holds a booking on answered 400 Bad Request. The guard threw
  a plain `BookingException` and the advice maps that to 400, so a well formed request was told
  it was malformed when the truth was that somebody booked that date first. It is now
  `BookingDateAlreadyBookedException`, answered with 409 Conflict and no `Retry-After`, which
  is a distinction a user interface can act on: 400 says do not send this again, 409 says try
  another date. A client that branched on 400 to mean a bad payload now sees 409 for this case.
- An exhausted event append retry escaped with no advice mapping, so it surfaced as a bare 500
  carrying Spring Boot's default error body instead of the RFC 9457 problem document every
  other error here returns. `EventSequenceConflictException` is now mapped to 409 Conflict,
  because the write lost a race on a sequence number rather than failing on the server. It
  sends no `Retry-After` on purpose: the conflict means another writer advanced the sequence,
  so waiting achieves nothing and the `detail` says to re-read the booking before retrying.
  That `detail` is a fixed sentence rather than the internal message, so nothing about the
  event log leaks to the caller.
- Ten lookups of an offer that does not exist opened the circuit breaker.
  `OfferNotFoundException` counted as a failure, so a run of 404s filled the ten call sliding
  window, the breaker opened, and every booking after that was answered as though sky-offer had
  gone down. The `offerService` breaker now records only `ResourceAccessException` and
  `IOException`, so a missing offer and a missing service can no longer collapse into one
  status.
- Two bookings racing on the same booking could write the same event sequence number, because
  the sequence was read and then written with nothing enforcing uniqueness in between. The
  `uq_booking_event_booking_seq` unique constraint now enforces it and the appender retries on
  the conflict, so the event stream for a booking is gapless and ordered.
- Event timestamps were stored in a type with no time zone, so a reader in a different zone
  read a different instant than the writer wrote. The column is `timestamptz` and the field is
  an `Instant`.
- Flyway did not actually run under Spring Boot 4, so the schema was whatever Hibernate had
  last generated. It runs now, with `baseline-on-migrate` and baseline version 0 so an existing
  database adopts the history without a manual repair.
