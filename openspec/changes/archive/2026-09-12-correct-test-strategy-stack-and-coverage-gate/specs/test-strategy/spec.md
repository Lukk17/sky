## MODIFIED Requirements

### Requirement: Integration tests use Testcontainers, not embedded substitutes
All `@SpringBootTest` integration tests that exercise persistence or Kafka MUST run against Testcontainers-managed PostgreSQL and Kafka instances, and MUST NOT run against an in-memory database such as H2, nor against an `@EmbeddedKafka` broker. The PostgreSQL image MUST be pinned, to `postgres:17-alpine`, in each data-bearing module's own `TestcontainersConfiguration`, so a schema change is validated against the engine major the tests claim to cover. A module MUST start only the containers its own stack needs: sky-booking and sky-offer need both, sky-message needs PostgreSQL alone because it declares no Kafka dependency, and sky-notify needs Kafka alone because it holds no persistent state.

#### Scenario: Running integration tests locally
- **WHEN** a developer runs `./gradlew :sky-booking:test` with Docker available
- **THEN** the integration tests start an ephemeral `postgres:17-alpine` container and an ephemeral Kafka container, applying real Flyway migrations and real Spring Kafka serialization

### Requirement: Coverage gate enforces a floor
A JaCoCo coverage verification task MUST run as part of `check`, and therefore as part of `build`, failing the build below 0.90 line coverage and 0.90 branch coverage. Both minima MUST be declared once for every module rather than per module, so no module can quietly hold a lower floor than the others. The set the floor is measured over excludes wire DTOs, configuration classes, the application class, and constants holders. A module that those exclusions leave with nothing to measure MUST disable the verification task explicitly in its own build file, because the alternative is a floor nobody can meet, and it MUST NOT be accommodated by lowering the shared minima.

#### Scenario: Coverage drops below threshold
- **WHEN** a change reduces line coverage below 0.90 over the measured set of any module that has one
- **THEN** `./gradlew check` fails with a JaCoCo coverage-verification error naming the violated counter, the measured ratio and the required minimum, reported over the whole module bundle because the violation rule sets no finer element

#### Scenario: A module has nothing left to measure
- **WHEN** every class in a module falls inside the exclusion set, as in sky-gateway, whose only classes are its application class and one configuration class
- **THEN** that module disables its own verification task in its own build file, the shared minima stay at 0.90, and every other module is still gated
