## Context

There are three honest test pyramids: unit, integration, E2E. The current suite is bottom-heavy on integration (8 `@SpringBootTest` classes for ~12–15 method bodies under test per service), shallow on unit (4 pure unit tests across the whole repo), and lacks an automated E2E loop entirely. Coverage of failure modes is the biggest gap — every test in the audit asserts the success path; "what if the REST call returns 503" is not tested anywhere.

The goal is not "more tests" but "the right tests": cheap fast unit tests for branches, focused slice tests for persistence, fewer but realistic integration tests with Testcontainers, and one automated E2E loop.

## Goals / Non-Goals

**Goals:**
- Every public method has at least one negative test.
- Repository custom queries are exercised against a real MySQL (Testcontainers).
- Controller security is tested both ways (authenticated and not).
- JaCoCo enforces a coverage floor in CI; coverage trend visible.
- E2E happens automatically every PR via Bruno.

**Non-Goals:**
- Mutation testing (pitest). Useful but a different change.
- Contract testing (Pact). Worth doing once we have multiple consumers per service; one consumer doesn't justify it.
- 100% coverage. 80% line + 70% branch is the floor; chasing 100% wastes time on getters.
- Rewriting existing passing tests to AssertJ. Only new code uses AssertJ.

## Decisions

1. **Testcontainers over Embedded.** H2-with-MySQL-mode is a partial truth; embedded Kafka is single-broker. Both diverge from prod under load. Testcontainers costs ~30s of startup; cached in CI.
2. **`AbstractIntegrationTest` base in `sky-common` test-fixtures.** Containers are static and shared per JVM (Testcontainers reuse) — fast across multiple test classes in the same module.
3. **`@DataJpaTest` uses the same Testcontainer.** Don't run `@DataJpaTest` against H2 just to be fast — gain is wiped out by the next bug it lets through.
4. **`@WithMockUser` adaptation**: the current code reads user identity from the `X-Forwarded-User` header (gateway-trust pattern). For tests, either keep the header-injection pattern OR (better) wire a `Principal`-based filter and remove the trust on inbound headers. Out of scope here; tests preserve current header-based contract.
5. **Negative paths via mocked ports.** Use Mockito at the port (interface) layer to simulate downstream failures. Avoid faking deep — testing 503 from sky-offer should mock the `OfferRestClient` port, not the HTTP transport.
6. **JaCoCo thresholds start at 80/70.** Initial coverage may be lower per service; ramp by setting per-module thresholds based on the post-backfill snapshot, not aspirational numbers. Don't push below 70/60 once stabilized.
7. **Bruno runs against docker-compose**, not Helm in CI. Helm-against-kind takes 5+ minutes; compose is sub-minute and exercises the same images.
8. **No mutation or contract testing.** Both are valuable but separate changes; pulling them in here would push this PR past readability.

## Risks / Trade-offs

- **Testcontainers needs Docker on the test host.** Local dev: developers run Docker Desktop anyway. CI: GitHub Actions Linux runners have Docker; macOS/Windows runners need workarounds.
- **JaCoCo gate on first run will fail** if thresholds are set higher than current coverage. Mitigation: set thresholds to current coverage + 5% as a ratchet, raise per release.
- **Test suite runtime increases** with Testcontainers. Mitigation: run unit/slice tests in `./gradlew test` and integration tests in `./gradlew integrationTest` (separate task) so day-to-day TDD stays snappy.
- **Bruno flakiness**: E2E against a live stack can flake on slow CI runners. Mitigation: explicit waits for `/actuator/health`, retry on connection refused.
- **Two assertion styles**: AssertJ in new tests, JUnit in old. Acceptable transition cost for a year.
