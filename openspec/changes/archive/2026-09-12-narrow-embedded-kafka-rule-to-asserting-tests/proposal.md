## Why

`test-strategy` forbids the embedded Kafka broker outright, and five test classes have declared it since before the
rule was written. The change archived as `2026-09-12-correct-test-strategy-stack-and-coverage-gate` deliberately left
the rule strict and reported the five, because resolving it meant either rewriting test code or relaxing a contract,
and neither belongs in a correction of a stale fact. The owner has now decided, and the decision is to narrow the rule.

The reason the rule is wider than the risk it was written against is that none of the five asserts through a broker.
Verified by reading all five classes rather than by trusting the earlier report:
`sky-booking/src/test/java/com/lukk/sky/booking/SkyBookingApplicationTests.java`,
`sky-booking/src/test/java/com/lukk/sky/booking/adapters/inbound/api/BookingControllerTest.java`,
`sky-offer/src/test/java/com/lukk/sky/offer/adapters/inbound/api/OfferApiControllerTest.java`,
`sky-offer/src/test/java/com/lukk/sky/offer/adapters/inbound/api/OfferApiDocumentTest.java` and
`sky-offer/src/test/java/com/lukk/sky/offer/DemoSeedMigrationTest.java`. None references a `KafkaTemplate`, a Kafka
producer or consumer, a `ConsumerRecord`, `KafkaTestUtils` or a `@KafkaListener`. The classes that do hold those
references are a disjoint set, and every one of them reaches a broker through a container rather than an embedded one.

Swapping five embedded brokers for containers therefore costs startup time on every build and buys no assertion. The
rule is narrowed to bind on what it was protecting: a test that makes a claim about a record.

## What Changes

- Restate the Testcontainers requirement so the Kafka half binds on tests that assert Kafka behaviour, and so the
  requirement says what asserting Kafka behaviour means, in terms a reader can check against a given test without
  guessing: publishing a record, consuming one, or inspecting one.
- Leave the persistence half exactly as strict as it is. No test may reach an in-memory database, the PostgreSQL image
  stays pinned to `postgres:17-alpine`, and none of the five classes breaks that half anyway: each one imports its
  module's `TestcontainersConfiguration`, so its database already comes from a container.
- Leave the prohibition in force for any test that does assert through a broker, so the narrowing removes no coverage.
- Add two scenarios, one per side of the line, so the boundary is a checkable rule rather than a judgement a reviewer
  has to reconstruct.

Nothing here changes code. The five test classes are deliberately untouched: narrowing the rule is the whole change.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `test-strategy`: the Kafka half of the requirement `Integration tests use Testcontainers, not embedded substitutes`,
  which becomes conditional on what a test asserts rather than absolute.

## Impact

- Affected file: `openspec/specs/test-strategy/spec.md`, rewritten at archive time from the delta in this change. One
  requirement of the five in that file is touched.
- The four requirements not named here were read and are not restated. Two of them were corrected earlier today, by
  `2026-09-12-correct-stale-spec-requirements` and by
  `2026-09-12-correct-test-strategy-stack-and-coverage-gate`, and keeping them out of this delta is what guarantees
  those corrections are not undone.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched, and no build is run. Every
  claim about the five classes comes from reading them.
- Cost, stated here because it is the price of the decision rather than a detail: the rule stops being a one-line grep
  for one annotation. Auditing it now means reading what a test asserts. The requirement carries the list of types
  that settle the question, so the reading is bounded, but it is reading.
- Risk: low on coverage, since a test that asserts through a broker is still held to a container. The residual risk is
  that a future test quietly grows a broker assertion while keeping its embedded broker, which the annotation-only
  audit would have caught and this one will not.
