## 1. Verify the five classes rather than trusting the report

- [x] 1.1 List every class declaring `@EmbeddedKafka` across all six module test trees, and verify the set is exactly
  the five named in proposal.md with no sixth
- [x] 1.2 For each of the five, verify it references no `KafkaTemplate`, no Kafka producer or consumer, no
  `ConsumerRecord`, no `KafkaTestUtils` and no `@KafkaListener`, so the claim that none asserts through a broker rests
  on a grep rather than on an impression
- [x] 1.3 Verify the set of test classes that do hold those references is disjoint from the five
- [x] 1.4 Verify each of the five imports its module's `TestcontainersConfiguration`, so the narrowing touches the
  broker half of the rule and leaves the database half untouched in practice as well as in text
- [x] 1.5 Verify how each of the five relates to its notification port, and record the finding even where it differs
  from the report received, so the requirement text describes what is actually there
- [x] 1.6 Verify the tests that do exercise Kafka reach a container, by reading each module's
  `AbstractIntegrationTest` and confirming it brings up a `ConfluentKafkaContainer`
- [x] 1.7 Establish whether the embedded broker is structurally required at context startup, and if that cannot be
  settled without a build run, record the weaker verified claim instead of the stronger unverified one

## 2. Write the delta specification

- [x] 2.1 Read the current `openspec/specs/test-strategy/spec.md` rather than assuming its content, because two
  changes archived earlier today modified it
- [x] 2.2 Verify the two requirements corrected earlier today are absent from this delta, so neither correction can be
  undone
- [x] 2.3 Write `specs/test-strategy/spec.md` under `## MODIFIED Requirements`, carrying the whole requirement block,
  and verify the requirement header and the existing scenario name match the merged file character for character
- [x] 2.4 Verify the persistence half, the pinned image and the per-module container list survive the restatement
  unchanged in force
- [x] 2.5 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic
  outside the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated
  against a fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 2.6 Verify the delta respects the wrap width of the merged file, which is one physical line per paragraph and
  per scenario bullet
- [x] 2.7 Run `openspec validate narrow-embedded-kafka-rule-to-asserting-tests --strict` and verify it reports no error

## 3. Archive and verify the merged file

- [x] 3.1 Archive the change and verify `openspec/specs/test-strategy/spec.md` still forbids an in-memory database and
  still names `postgres:17-alpine`
- [x] 3.2 Verify the merged file now permits the cheapest broker stand-in for a test that asserts nothing through a
  broker, and still requires a Testcontainers broker for a test that asserts Kafka behaviour
- [x] 3.3 Verify the merged file still carries the status set from `2026-09-12-correct-stale-spec-requirements`, by
  checking the 409 line, the 503 line and the `POST /api/v1/bookings` path are all still present
- [x] 3.4 Verify the merged file still carries the coverage numbers from
  `2026-09-12-correct-test-strategy-stack-and-coverage-gate`, by checking 0.90 line and 0.90 branch and the wiring into
  `check` are all still present
- [x] 3.5 Verify the five test classes are unmodified, because narrowing the rule was the whole change
- [x] 3.6 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 4. Notes from the run

- Task 1.1 returned exactly the five classes named in proposal.md and no sixth, over all six module test trees.
- Task 1.2 returned zero matches per class for the whole type list, so none of the five asserts through a broker.
- Task 1.3 holds. The classes that do hold those references are `BookingIntegrationTest`, `OfferIntegrationTest`,
  `KafkaCorrelationIdIntegrationTest`, `KafkaConsumerConfigTest`, `sky-notify`'s `AbstractIntegrationTest` and
  `sky-common`'s `KafkaNotificationPublisherTest`. No class appears in both sets.
- Task 1.5 found one detail the report received did not have exactly right. Three of the five mock their notification
  port: `BookingControllerTest` mocks `BookingNotificationService`, `OfferApiControllerTest` and
  `OfferApiDocumentTest` mock `OfferNotificationService`. The other two mock nothing at all.
  `SkyBookingApplicationTests` has one empty test body asserting only that the context loads, and
  `DemoSeedMigrationTest` asserts seeded rows through `OfferRepository` and `EventSourceRepository`. The conclusion is
  unaffected and arguably stronger, because a test that publishes nothing is further from a broker than a test whose
  publisher is mocked. The requirement text covers both shapes rather than only the mocking one.
- Task 1.6 holds for both modules. `sky-booking`'s `AbstractIntegrationTest` brings up a `PostgreSQLContainer` on
  `TestcontainersConfiguration.POSTGRES_IMAGE` and a `ConfluentKafkaContainer` on
  `TestcontainersConfiguration.KAFKA_IMAGE`. `sky-offer`'s brings up the same pair, though it parses
  `confluentinc/cp-kafka:7.6.0` inline rather than reading it from its `TestcontainersConfiguration`, which is an
  inconsistency between the two modules rather than a rule violation. Reported, not changed: this change touches no
  test code.
- Task 1.7 could not be settled without a build run, so the weaker verified claim is what the requirement states.
  Neither `sky-booking` nor `sky-offer` declares a `@KafkaListener` in its main tree, and the only Kafka bean either
  one injects is a `KafkaTemplate`, whose producer factory is lazy. Under the `test` profile neither module's
  `application-test.yaml` sets `spring.kafka.bootstrap-servers`, so the embedded broker is what supplies the address.
  Whether the context would start without it is untested here and the requirement does not claim it either way.
