## MODIFIED Requirements

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
