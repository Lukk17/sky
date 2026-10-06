## 1. Testcontainers

- [x] 1.1 Add `testcontainers-bom`, `testcontainers-junit-jupiter`, `testcontainers-mysql`, `testcontainers-kafka` to the catalog.
- [x] 1.2 Wire them into `sky.spring-service-conventions` for test scope.
- [x] 1.3 Create `sky-common` test-fixture: `AbstractIntegrationTest` with `@Testcontainers` and static `MySQLContainer` + `KafkaContainer`, `@DynamicPropertySource` registering JDBC URL and Kafka bootstrap servers.
- [x] 1.4 Migrate `BookingIntegrationTest`, `OfferIntegrationTest`, `MessageIntegrationTest` to extend `AbstractIntegrationTest`. Remove `@EmbeddedKafka` and H2 imports.
- [x] 1.5 Verify CI runners have Docker available (or document the Testcontainers Cloud / desktop requirement in README).

## 2. Repository slice tests

- [x] 2.1 sky-booking: `BookingRepositoryTest` + `EventSourceRepositoryTest` with `@DataJpaTest`. Cover happy queries and the custom HQL `max(sequenceNumber)`.
- [x] 2.2 sky-offer: same pattern.
- [x] 2.3 sky-message: `MessageRepositoryTest`.
- [x] 2.4 Use Testcontainers MySQL (not in-memory H2) so the SQL dialect tested matches production.

## 3. Security tests

- [x] 3.1 Audit current controller tests; for each, add an unauthenticated variant asserting 401/403 (or 400 if the existing flow uses missing X-Forwarded-User as "bad request" semantics — preserve existing behavior).
- [x] 3.2 Use `@WithMockUser(username = TEST_USER_EMAIL)` or build a `JwtAuthenticationToken` for tests where the X-Forwarded-User header was the auth signal.
- [x] 3.3 Add a "wrong user" variant where applicable (e.g., deleting another user's booking → 403).

## 4. Negative-path coverage on services

- [x] 4.1 For every `*ServicePrimary` class, add tests for each public method covering: not-found → exception, validation failure, downstream port throwing.
- [x] 4.2 For sky-booking's offer-lookup: REST client returns 4xx → expected exception type; REST client times out → expected exception type.
- [x] 4.3 For Kafka producers: `KafkaTemplate.send` future fails → service surfaces a usable exception.

## 5. sky-notify backfill

- [x] 5.1 Unit-test `WebSocketService.triggerMessage` — verify `SimpMessagingTemplate.convertAndSendToUser` called with expected destination and payload.
- [x] 5.2 Unit-test `NotificationPublisherPrimary.publish` — verify it dispatches to the right downstream and on failure throws/logs appropriately.
- [x] 5.3 Integration test: produce to embedded/testcontainers Kafka → assert WS message visible to a `WebSocketStompClient` test client (uses Spring's `WebSocketTestServer`-like setup).
- [x] 5.4 Negative: poison pill on the Kafka topic → routed to DLT (depends on `kafka-reliability`).

## 6. JaCoCo

- [x] 6.1 Apply `jacoco` plugin in `sky.spring-service-conventions`.
- [x] 6.2 Configure `tasks.test { finalizedBy(tasks.jacocoTestReport) }`.
- [x] 6.3 Add `jacocoTestCoverageVerification` with thresholds: 80% line, 70% branch. Initial run may need slightly lower thresholds; raise after backfill.
- [x] 6.4 Add `check.dependsOn(jacocoTestCoverageVerification)` so `./gradlew build` fails on uncovered modules.
- [x] 6.5 Configure exclusions: `**/dto/**`, `**/config/**`, `*Application.java`, generated Q-types (none here yet but harmless).

## 7. Bruno in CI

- [x] 7.1 Add `.github/workflows/e2e.yaml` (or equivalent) that:
  - Spins up the stack (docker-compose preferred for CI speed; Helm against kind cluster as alternative).
  - Waits for `/actuator/health` on each service.
  - Runs `bru run -r --env <env> --insecure` from `docs/api/request` — pick the localhost env file.
  - Fails on non-zero exit.
- [x] 7.2 Document how to run Bruno locally in root README.

## 8. AssertJ

- [x] 8.1 Add `assertj-core` to the catalog and convention plugin.
- [x] 8.2 Use AssertJ in all NEW tests added by this change.
- [x] 8.3 Do not refactor existing JUnit asserts unless touching those files for other reasons.

## 9. Verify

- [x] 9.1 `./gradlew test` — green across services.
- [x] 9.2 `./gradlew jacocoTestCoverageVerification` — meets thresholds.
- [x] 9.3 Bruno run green against a freshly-deployed stack.
- [x] 9.4 Coverage report links surfaced in CI artifacts.
