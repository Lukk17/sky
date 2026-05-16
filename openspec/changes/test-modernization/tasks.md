## 1. Testcontainers

- [ ] 1.1 Add `testcontainers-bom`, `testcontainers-junit-jupiter`, `testcontainers-mysql`, `testcontainers-kafka` to the catalog.
- [ ] 1.2 Wire them into `sky.spring-service-conventions` for test scope.
- [ ] 1.3 Create `sky-common` test-fixture: `AbstractIntegrationTest` with `@Testcontainers` and static `MySQLContainer` + `KafkaContainer`, `@DynamicPropertySource` registering JDBC URL and Kafka bootstrap servers.
- [ ] 1.4 Migrate `BookingIntegrationTest`, `OfferIntegrationTest`, `MessageIntegrationTest` to extend `AbstractIntegrationTest`. Remove `@EmbeddedKafka` and H2 imports.
- [ ] 1.5 Verify CI runners have Docker available (or document the Testcontainers Cloud / desktop requirement in README).

## 2. Repository slice tests

- [ ] 2.1 sky-booking: `BookingRepositoryTest` + `EventSourceRepositoryTest` with `@DataJpaTest`. Cover happy queries and the custom HQL `max(sequenceNumber)`.
- [ ] 2.2 sky-offer: same pattern.
- [ ] 2.3 sky-message: `MessageRepositoryTest`.
- [ ] 2.4 Use Testcontainers MySQL (not in-memory H2) so the SQL dialect tested matches production.

## 3. Security tests

- [ ] 3.1 Audit current controller tests; for each, add an unauthenticated variant asserting 401/403 (or 400 if the existing flow uses missing X-Forwarded-User as "bad request" semantics — preserve existing behavior).
- [ ] 3.2 Use `@WithMockUser(username = TEST_USER_EMAIL)` or build a `JwtAuthenticationToken` for tests where the X-Forwarded-User header was the auth signal.
- [ ] 3.3 Add a "wrong user" variant where applicable (e.g., deleting another user's booking → 403).

## 4. Negative-path coverage on services

- [ ] 4.1 For every `*ServicePrimary` class, add tests for each public method covering: not-found → exception, validation failure, downstream port throwing.
- [ ] 4.2 For sky-booking's offer-lookup: REST client returns 4xx → expected exception type; REST client times out → expected exception type.
- [ ] 4.3 For Kafka producers: `KafkaTemplate.send` future fails → service surfaces a usable exception.

## 5. sky-notify backfill

- [ ] 5.1 Unit-test `WebSocketService.triggerMessage` — verify `SimpMessagingTemplate.convertAndSendToUser` called with expected destination and payload.
- [ ] 5.2 Unit-test `NotificationPublisherPrimary.publish` — verify it dispatches to the right downstream and on failure throws/logs appropriately.
- [ ] 5.3 Integration test: produce to embedded/testcontainers Kafka → assert WS message visible to a `WebSocketStompClient` test client (uses Spring's `WebSocketTestServer`-like setup).
- [ ] 5.4 Negative: poison pill on the Kafka topic → routed to DLT (depends on `kafka-reliability`).

## 6. JaCoCo

- [ ] 6.1 Apply `jacoco` plugin in `sky.spring-service-conventions`.
- [ ] 6.2 Configure `tasks.test { finalizedBy(tasks.jacocoTestReport) }`.
- [ ] 6.3 Add `jacocoTestCoverageVerification` with thresholds: 80% line, 70% branch. Initial run may need slightly lower thresholds; raise after backfill.
- [ ] 6.4 Add `check.dependsOn(jacocoTestCoverageVerification)` so `./gradlew build` fails on uncovered modules.
- [ ] 6.5 Configure exclusions: `**/dto/**`, `**/config/**`, `*Application.java`, generated Q-types (none here yet but harmless).

## 7. Newman in CI

- [ ] 7.1 Add `.github/workflows/e2e.yml` (or equivalent) that:
  - Spins up the stack (docker-compose preferred for CI speed; Helm against kind cluster as alternative).
  - Waits for `/actuator/health` on each service.
  - Runs `newman run config/postman-collection/sky.postman_collection.json -e config/postman-collection/<env>` — pick the localhost env file.
  - Fails on non-zero exit.
- [ ] 7.2 Document how to run Newman locally in root README.

## 8. AssertJ

- [ ] 8.1 Add `assertj-core` to the catalog and convention plugin.
- [ ] 8.2 Use AssertJ in all NEW tests added by this change.
- [ ] 8.3 Do not refactor existing JUnit asserts unless touching those files for other reasons.

## 9. Verify

- [ ] 9.1 `./gradlew test` — green across services.
- [ ] 9.2 `./gradlew jacocoTestCoverageVerification` — meets thresholds.
- [ ] 9.3 Newman job green against a freshly-deployed stack.
- [ ] 9.4 Coverage report links surfaced in CI artifacts.
