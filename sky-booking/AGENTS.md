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
- `POSTGRES_USER` and `POSTGRES_PASSWORD` have no default in `application.yaml` and must not gain one: the
  `spring-boot-hygiene` specification grants its credential-default exemption to `local` profile files alone, and
  `application-local.yaml` already defaults both. An unset variable now fails startup through
  `DatasourceCredentialsAutoConfiguration` in sky-common, naming the variable and the property, instead of
  binding the literal placeholder text as the password. `DatasourceCredentialsStartupTest` pins that behaviour
  against this module's own configuration files, so do not delete it when touching the datasource block.
- The domain service publishes the Kafka event, never the controller. `BookingServicePrimary` calls
  `BookingNotificationService` at the end of `bookOffer` and at the end of each branch of `removeBooking`, and
  `BookingController` injects no outbound port at all. It used to inject one, build the `KafkaPayloadModel` itself and
  publish after the service returned, which put the decision to announce a booking one layer above the rules that
  decide whether there is a booking. The driven port now names the event (`publishCreated`, `publishRemoved`) and the
  adapter owns the envelope, the timestamp and the serialisation, so `KafkaPayloadModel` and the `ObjectMapper` are
  gone from both the controller and the domain. `removeBooking` returns `void` for the same reason: the confirmation
  string it used to return existed only so the controller could publish it.
- Moving the call changed the ordering on one of the two sites, and the difference is deliberate rather than an
  oversight. `bookOffer` carries no `@Transactional`: its transaction lives one bean deeper in
  `BookingPersister.saveAndPublish`, which is a separate bean and therefore a real proxy boundary, so publishing after
  that call still happens after the commit, exactly as the controller's did. `removeBooking` is `@Transactional` on
  the method, so its publication now runs inside the transaction where it used to run after it. The residual that
  creates is the mirror of the old one. Before, a committed delete could end up with no event, because
  `KafkaNotificationPublisher` logs a failed send at warn and swallows it. Now, an event can be handed to the producer
  and the commit can then fail, leaving an event for a booking that still exists. The window is one commit wide, it is
  the same family as the accepted storage residual in sky-offer's guide, and the fix for it is a transactional outbox
  that nobody has asked for. Keep the publish last in the method, after every domain step, so anything the domain
  rejects still publishes nothing. Do not move it earlier, which widens the window rather than closing it.
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
  that map so both validations above actually run. sky-offer and sky-notify carry the identical five values, so
  change all three or none.
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
