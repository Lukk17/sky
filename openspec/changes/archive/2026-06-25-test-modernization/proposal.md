## Why

The test suite has the right scaffolding (Spring Boot Test, `spring-kafka-test`, `spring-security-test`, mockwebserver) but uses it shallowly:

- **82 tests across 4 services** — wide but not deep. Heavy on integration; thin on negative paths.
- **sky-notify has 2 tests for 15+ production classes.** WebSocketService, NotificationPublisherPrimary, NotificationTransmissionServicePrimary have effectively no coverage.
- **Zero `@WithMockUser` tests** despite `spring-security-test` on the classpath. Auth-protected endpoints have never been tested under both authenticated AND unauthenticated conditions.
- **Zero `@DataJpaTest` slice tests.** Repositories are untested in isolation.
- **No Testcontainers.** H2 with `MODE=MySQL` is a reasonable proxy but loses real-world MySQL behavior (charset, AUTO_INCREMENT, GIS, JSON ops) — production-only bugs slip through.
- **No JaCoCo, no coverage gate.** Coverage is unknown and unmeasured.
- **E2E is manual Bruno.** No automated end-to-end signal in CI.
- **No negative-path coverage on Kafka consumers.** Poison pills, deserialization failures, downstream emit failures — untested.

The user explicitly asked for full logic coverage including error and negative paths. This change pulls the suite up to that bar.

## What Changes

- **Adopt Testcontainers** for MySQL and Kafka. Replace `@EmbeddedKafka` + H2 in `@SpringBootTest` classes with `@Testcontainers` + `@DynamicPropertySource`. Use `org.testcontainers:mysql` and `org.testcontainers:kafka`.
- **Add `@DataJpaTest`** slice tests for every repository in booking, offer, message. Cover custom queries (e.g., the `SELECT max(e.sequenceNumber)` HQL).
- **Add `@WithMockUser`** to every controller test: a positive case (authenticated, expected user) and a negative case (unauthenticated → 401/403). Adapt for the X-Forwarded-User header convention currently used.
- **sky-notify backfill**: tests for `WebSocketService.triggerMessage` (assert delivery to expected destination), `NotificationPublisherPrimary` (assert routing decision), end-to-end Kafka → WS via test broker.
- **Negative-path coverage** for every public method on every primary service: invalid input, missing entity, downstream failure (`HttpServerErrorException` from REST client), Kafka producer send failure. Mocked at the port layer.
- **Add JaCoCo** plugin in convention plugin. Gates: 80% line, 70% branch per module. Fail the build below threshold.
- **Add Bruno** (Git-native API client) execution in CI: a GitHub Actions job that spins up the Helm stack (or compose) and runs the existing Bruno collection, replacing manual E2E.
- **AssertJ migration** for new tests (existing JUnit asserts left alone unless touched).

## Capabilities

### New Capabilities
- `test-strategy`: per-service test layering (unit / slice / integration / E2E), Testcontainers as the integration substrate, security tests via `@WithMockUser`, JaCoCo coverage gate, automated Bruno E2E.

### Modified Capabilities
- _None._

## Impact

- **Touched files**: ~30 new test classes across services; convention plugin update for JaCoCo; CI workflow file under `.github/workflows/` (assuming GitHub Actions — verify in apply).
- **Build time**: Testcontainers adds Docker image pull on first run. Acceptable for CI; locally cached.
- **CI runtime**: Bruno E2E adds maybe 2–5 minutes. Worth it.
- **Risk**: low. Tests can only fail; they cannot regress production code.
- **Dependency order**: depends on `hexagonal-enforcement-archunit` (ArchUnit tests live alongside; mass file moves create import churn). Should ideally land after `extract-sky-common` (so DLT and producer/consumer config to test are in stable locations). JaCoCo gate is best added near the end so it doesn't block earlier in-progress changes — or set generous initial thresholds and ratchet them up.
