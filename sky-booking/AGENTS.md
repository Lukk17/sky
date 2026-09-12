# AGENTS.md: sky-booking

Module-local guidance for `sky-booking`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-booking` is the bookings service: it manages bookings placed against offers. It runs on port 5555
(`BOOKING_PORT`, default 5555), exposes REST under the `/api/v1` prefix, and is one of the four independently-deployable
services. The module version is not restated here: the topmost `## [x.y.z]` entry in [CHANGELOG.md](CHANGELOG.md) is
the current one, and it wins over the cosmetic `version` in `build.gradle.kts`.

## Architecture

- Build plugin: `sky.spring-service-conventions` (Spring Boot app, fat `bootJar` named `sky-booking.jar`, JaCoCo
  report). Depends on `:sky-common`.
- Hexagonal layout (`com.lukk.sky.booking`):
  - `domain/model`, `domain/exception`: core model and domain exceptions.
  - `domain/ports/inbound`: driving port interfaces called by controllers (`BookingService`).
  - `domain/ports/outbound`: driven port interfaces implemented by infrastructure adapters (`BookingRepository`,
    `EventSourceRepository`, `BookingNotificationService`, `RestClient`, `RequestUriStrategy`).
  - `domain/service`: domain service implementations (`BookingServicePrimary`, `BookingPersister`,
    `EventSourceService`, `EventSourceServicePrimary`).
  - `adapters/inbound/api`: REST controllers; `adapters/dto`: wire DTOs; `adapters/outbound/rest`: the
    outbound offer-service client, caller, and URI strategy; `adapters/outbound/notification`: outbound notification.
  - `config`, `config/kafka`, `config/propertyBind`: Spring wiring and bound properties.
- Synchronous stack: the WebFlux migration is done. `BookingService` returns plain types (`BookingDTO` and
  `Page<BookingDTO>`, no `Mono`), and inter-service calls go through Spring's blocking `RestClient`, built in
  `config/RestClientConfig` on a `JdkClientHttpRequestFactory` with a 2s connect and 3s read timeout. Do not
  reintroduce `WebClient` or `spring-boot-starter-webflux` here.
- Resilience: `OfferServiceCaller` carries the Resilience4j `@Retry` and `@CircuitBreaker` annotations (instance
  `offerService`, tuned in `application.yaml`) and `OfferRestClient` translates an exhausted retry into an
  `OfferServiceUnavailableException`. The split into two beans is deliberate: Resilience4j works through Spring AOP
  proxies, so a self-call would bypass the annotations. `OfferServiceCallerResilienceTest` covers it.
- A dependency outage is 503 here, never 400. The offer lookup has four distinguishable outcomes and each one has
  its own exception type, so the mapping in `GlobalExceptionHandler` cannot drift:
  - `OfferServiceUnavailableException` is 503 with `Retry-After: 10`. It covers a refused connection, a connect
    timeout, a read timeout, a 5xx after the three attempts are exhausted, and an open circuit breaker. It does not
    extend `BookingException`, which is what stops a `catch (BookingException)` anywhere from swallowing an outage and
    what keeps it off the 400 branch. Do not fold it back into `BookingException`.
  - `OfferNotFoundException` is 404. A missing offer and a missing service are different answers and must stay so.
  - `OfferServiceBadResponseException` is 502, for any client-error status from sky-offer other than 404. The caller's
    request was well formed, so 400 would blame the wrong party, and the condition is not transient, so 503 with a
    `Retry-After` would invite a pointless retry.
  - `BookingException` stays 400 and is reserved for a request that really is wrong. The one guard left on it is the
    date-in-the-past check in `BookingServicePrimary`.
- A lost race is 409 here, never 400 and never a bare 500. Two exception types carry it, neither extending
  `BookingException`, for the same reason the outage type does not: a general catch must not be able to swallow them
  and the 400 mapping must not be able to reclaim them.
  - `BookingDateAlreadyBookedException` is 409, thrown by the already-booked guard in `BookingPersister`. The request
    is well formed and somebody simply got there first, so the honest answer is that the caller should pick another
    date, not that the request was malformed. It answered 400 before and that was the defect.
  - `EventSequenceConflictException` is 409 too, thrown by `EventSourceServicePrimary` once twenty append attempts have
    all lost the sequence number. It had no advice mapping at all before, so it surfaced as a bare 500 with Spring
    Boot's default error body rather than a problem detail.
  - Neither carries `Retry-After`, and that is the deliberate difference from the 503. `Retry-After` tells a caller
    that waiting is enough, which is true of an outage and false of a conflict: the state moved, so the caller has to
    re-read before it can send a write that could succeed. The sequence-conflict `detail` says exactly that, and it is
    a fixed sentence rather than the internal message, so the event-log internals stay out of the response.
- The circuit breaker records transport failures only. `record-exceptions` on the `offerService` instance lists
  `ResourceAccessException` and `IOException`, so a run of 404s cannot open the circuit. Without that, ten lookups of
  an offer that does not exist filled the ten call window, opened the breaker, and every later booking answered as an
  outage. `OfferServiceCallerResilienceTest` pins both halves: a full window of 404s leaves the circuit closed, a full
  window of transport failures opens it.
- Persistence: PostgreSQL (`org.postgresql:postgresql`, runtime, with `flyway-database-postgresql`) against the
  shared `sky` database, versioned with Flyway (`src/main/resources/db/migration/`, now V1 and V2). Tests run
  against a Testcontainers `postgres:17-alpine` container, never an in-memory database. H2 is gone from this module,
  from the shared test stack, and from the version catalogue.
- Messaging: produces/consumes Kafka events via `spring-kafka`, serialised with the Spring-managed Jackson 3
  `ObjectMapper` (`tools.jackson.databind.ObjectMapper`). Gson is gone from this module and from its build file.
- API docs: springdoc `webmvc` UI at `/swagger-ui/index.html`.

## Testing

Inherits the convention test stack (Spring Boot Test, Spring Security Test, JUnit 5, ArchUnit). Integration and
repository tests use Testcontainers PostgreSQL + Kafka (`@ServiceConnection`, no `@DynamicPropertySource`).
`spring-kafka-test` and `okhttp3 mockwebserver` cover messaging and outbound HTTP. Run from the repo root with the
single composite `./gradlew`, e.g. `./gradlew :sky-booking:test`.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here.
- Keep the hexagonal direction: controllers and adapters depend inward on `domain/ports/inbound` and `domain/ports/outbound`;
  `domain/service` implementations depend on those port interfaces. Adapters must never import from `domain/service`
  directly. ArchUnit enforces this. A violation fails the build, do not weaken the rule.
