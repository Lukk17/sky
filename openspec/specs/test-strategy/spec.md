# test-strategy Specification

## Purpose
Sets the bar every module's tests are held to, covering what they run against, which paths they must cover, the statuses a rejection is asserted as, the coverage floor the build enforces, and how an end-to-end flow is exercised.

## Requirements

### Requirement: Integration tests use Testcontainers, not embedded substitutes
Every `@SpringBootTest` test that exercises persistence MUST run against a Testcontainers-managed PostgreSQL instance, and MUST NOT run against an in-memory database such as H2. The PostgreSQL image MUST be pinned, to `postgres:17-alpine`, in each data-bearing module's own `TestcontainersConfiguration`, so a schema change is validated against the engine major the tests claim to cover.

A test that asserts Kafka behaviour MUST run against a Testcontainers-managed broker, and MUST NOT assert through an `@EmbeddedKafka` broker. A test asserts Kafka behaviour when it publishes a record, consumes a record, or inspects one, which on this codebase means it references a `KafkaTemplate`, a Kafka producer or consumer, a `ConsumerRecord`, `KafkaTestUtils`, or the `@KafkaListener` method under test. A test that needs the Kafka beans only so the application context can start, and that asserts nothing travelling through a broker, MAY use the cheapest broker stand-in that works, `@EmbeddedKafka` included. A test whose notification port is replaced by a mock falls on that second side by construction, because nothing it asserts reaches a broker at all. Which side a given test falls on is settled by what the test asserts, not by which annotations it carries.

A module MUST start only the containers its own stack needs: sky-booking and sky-offer need both, sky-message needs PostgreSQL alone because it declares no Kafka dependency, and sky-notify needs Kafka alone because it holds no persistent state.

#### Scenario: Running integration tests locally
- **WHEN** a developer runs `./gradlew :sky-booking:test` with Docker available
- **THEN** the tests that exercise persistence or assert Kafka behaviour start an ephemeral `postgres:17-alpine` container and an ephemeral Kafka container, applying real Flyway migrations and real Spring Kafka serialization

#### Scenario: A test asserts that a record reached the broker
- **WHEN** a test publishes to a topic and then reads the record back, or consumes a record its listener was expected to receive
- **THEN** the broker it runs against is a Testcontainers-managed one, and an `@EmbeddedKafka` broker is not accepted for that test whatever the rest of its context looks like

#### Scenario: A test needs the Kafka beans only while the context starts
- **WHEN** a context-level or controller-level test boots the application with its notification port mocked, asserts nothing about any record, and needs a broker address only so the Kafka beans can be built
- **THEN** the cheapest broker stand-in that makes the context start is accepted, including `@EmbeddedKafka`, and the test is still held to a Testcontainers PostgreSQL container for everything it asserts about persistence

### Requirement: Every public method has a negative-path test
Every public method on every `*ServicePrimary` and every controller method MUST have at least one test covering an error path (not-found, validation failure, downstream failure, malformed input, or unauthorized).

#### Scenario: A downstream service is unreachable
- **WHEN** sky-booking's offer-lookup REST client receives a 5xx response
- **THEN** a unit test verifies the booking service surfaces a defined exception type with a clear message

### Requirement: Authenticated and unauthenticated paths are both tested
Every controller test class MUST include at least one test where the request is authenticated and one where it is not. The authentication mechanism in tests MUST match the production mechanism, which is the JWT resource-server chain every service installs, so a test authenticates by presenting a JWT rather than by setting an identity header. A test asserting a rejection MUST assert the status the service's exception handler actually maps that failure to, not a placeholder pair.

#### Scenario: Unauthenticated request to a protected endpoint
- **WHEN** a test sends a request to `POST /api/v1/bookings` with no bearer token
- **THEN** the controller returns 401 and the test asserts that status

#### Scenario: A rejection is asserted as the status its handler returns
- **WHEN** a controller test in `sky-booking` or `sky-offer` asserts the status of a rejected request
- **THEN** the asserted status is the one that service's exception handler maps the failure to, drawn from this set:
  - 400 for a domain exception, a bean-validation failure, an unknown sort property, or a value the database refused
  - 401 for a missing or invalid bearer token
  - 403 for an access-denied failure
  - 404 for a booking or an offer that does not exist
  - 409 for an event-sequence conflict in either service, and for an already-booked date in `sky-booking`
  - 502 for a dependency that answered unusably
  - 503 carrying a `Retry-After` header for a dependency outage

### Requirement: Coverage gate enforces a floor
A JaCoCo coverage verification task MUST run as part of `check`, and therefore as part of `build`, failing the build below 0.90 line coverage and 0.90 branch coverage. Both minima MUST be declared once for every module rather than per module, so no module can quietly hold a lower floor than the others. The set the floor is measured over excludes wire DTOs, configuration classes, the application class, and constants holders. A module that those exclusions leave with nothing to measure MUST disable the verification task explicitly in its own build file, because the alternative is a floor nobody can meet, and it MUST NOT be accommodated by lowering the shared minima.

#### Scenario: Coverage drops below threshold
- **WHEN** a change reduces line coverage below 0.90 over the measured set of any module that has one
- **THEN** `./gradlew check` fails with a JaCoCo coverage-verification error naming the violated counter, the measured ratio and the required minimum, reported over the whole module bundle because the violation rule sets no finer element

#### Scenario: A module has nothing left to measure
- **WHEN** every class in a module falls inside the exclusion set, as in sky-gateway, whose only classes are its application class and one configuration class
- **THEN** that module disables its own verification task in its own build file, the shared minima stay at 0.90, and every other module is still gated

### Requirement: E2E tests run against a freshly-deployed stack
The Bruno collection MUST be runnable against a freshly-deployed stack (Docker Compose or an in-cluster Kubernetes deployment), failing on any assertion failure. The end-to-end flows are driven through the OpenSpec e2e runbooks under `e2e/testing/`.

#### Scenario: A regression breaks an end-to-end flow
- **WHEN** a code change breaks the booking-creation flow such that the Bruno assertion for the create-booking request fails
- **THEN** the `bru run` exits non-zero and the e2e runbook is marked failed
