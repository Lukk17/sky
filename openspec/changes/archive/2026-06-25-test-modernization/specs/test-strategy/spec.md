## ADDED Requirements

### Requirement: Integration tests use Testcontainers, not embedded substitutes
All `@SpringBootTest` integration tests that exercise persistence or Kafka MUST run against Testcontainers-managed MySQL and Kafka instances, not against H2 or `@EmbeddedKafka`.

#### Scenario: Running integration tests locally
- **WHEN** a developer runs `./gradlew :sky-booking:test` with Docker available
- **THEN** the integration tests start ephemeral MySQL and Kafka containers, applying real Flyway migrations and real Spring Kafka serialization

### Requirement: Every public method has a negative-path test
Every public method on every `*ServicePrimary` and every controller method MUST have at least one test covering an error path (not-found, validation failure, downstream failure, malformed input, or unauthorized).

#### Scenario: A downstream service is unreachable
- **WHEN** sky-booking's offer-lookup REST client receives a 5xx response
- **THEN** a unit test verifies the booking service surfaces a defined exception type with a clear message

### Requirement: Authenticated and unauthenticated paths are both tested
Every controller test class MUST include at least one test where the request is authenticated and one where it is not. The authentication mechanism in tests MUST match the production mechanism (X-Forwarded-User header today; JWT after `websocket-auth`).

#### Scenario: Unauthenticated request to a protected endpoint
- **WHEN** a test sends a request to `POST /api/bookings` with no user identity header
- **THEN** the controller returns the expected rejection (400 or 401 per current contract) and the test asserts this

### Requirement: Coverage gate enforces a floor
A JaCoCo coverage verification task MUST run as part of `./gradlew build`, failing the build below 80% line coverage and 70% branch coverage per service module. DTOs, configuration classes, and generated code are excluded.

#### Scenario: Coverage drops below threshold
- **WHEN** a change reduces test coverage below 80% line in any service
- **THEN** `./gradlew build` fails with a JaCoCo coverage-verification error and a per-package breakdown

### Requirement: E2E tests run automatically in CI
The Bruno collection MUST be runnable via the Bruno CLI in CI against a freshly-deployed stack (docker-compose or kind), failing the pipeline on any assertion failure.

#### Scenario: A regression breaks an end-to-end flow
- **WHEN** a code change breaks the booking-creation flow such that the Bruno test for `POST /api/bookings` fails
- **THEN** the CI Bruno run exits non-zero and the PR check fails
