## Context

See proposal.md, Why, for the motivation. Four constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops
a scenario name the merged requirement already has. The one existing scenario name,
`Running integration tests locally`, is carried through unchanged, and the two new scenarios are additions inside the
restated block rather than renames.

The file was corrected twice earlier today. `2026-09-12-correct-stale-spec-requirements` rewrote
`Authenticated and unauthenticated paths are both tested`, and
`2026-09-12-correct-test-strategy-stack-and-coverage-gate` rewrote the coverage requirement and the Testcontainers one.
This delta restates only the Testcontainers requirement, so both of the other corrections stand untouched.

The narrowing is the owner's decision, not a finding. The evidence pass behind it could only establish that no test
currently breaks the spirit of the rule. It could not decide whether the rule should change, which is why the earlier
change left it strict.

## Goals / Non-Goals

**Goals:**

- Make the Kafka half of the rule bind on the behaviour it was protecting, which is an assertion about a record,
  rather than on an annotation that happens to correlate with it.
- Write the boundary so a reader can place a given test on one side of it by reading the test. A rule whose
  application needs a conversation is a rule that will be applied inconsistently.
- Keep the persistence half, the pinned image and the per-module container list exactly as they are, so this change
  relaxes one thing and nothing else.

**Non-Goals:**

- Editing the five test classes. Their embedded brokers are now permitted, so there is nothing to fix, and the
  instruction for this change was explicit that narrowing the rule is the whole of it.
- The four other requirements in this file. None of them mentions Kafka, none needs restating to carry this change,
  and restating a block obliges correcting every stale detail inside it, which needs an evidence pass this change does
  not have behind those four.
- Deciding whether the five classes need an embedded broker at all. See Risks.

## Decisions

Draw the line at what a test asserts, and enumerate the types that settle it. The alternative was to draw it at what
a test is for, something like permitting the embedded broker in a context-load test. That fails on the first test that
is mostly a controller test and incidentally reads a record, because its purpose argues one way and its assertion the
other. Naming the types means a reviewer greps a candidate test for `KafkaTemplate`, a producer or consumer,
`ConsumerRecord`, `KafkaTestUtils` and `@KafkaListener`, and the answer is the same whoever runs the grep. That list
is exactly the one the evidence pass used, and on the five classes it returned zero.

Say which side a mocked notification port falls on, explicitly. Three of the five classes replace
`BookingNotificationService` or `OfferNotificationService` with a mock, which is the clearest possible case of a test
that cannot reach a broker, and saying so in the requirement saves the next reader the inference. The other two,
`SkyBookingApplicationTests` and `DemoSeedMigrationTest`, mock nothing at all: the first asserts only that the context
loads and the second asserts rows through two JPA repositories. Neither publishes anything either, so both land on the
same side for a simpler reason than mocking.

Keep the prohibition as a prohibition for the asserting case. A narrowed rule that merely prefers a container would
lose the original point, which is that an assertion about broker behaviour made against an embedded broker is an
assertion about a different broker than the one production runs.

Do not claim the embedded broker is required for those contexts to start. It supplies a bootstrap address the Kafka
beans can be built against, and neither `sky-booking` nor `sky-offer` declares a `@KafkaListener`, so their producer
factories are lazy and a context might well start without any broker at all. Establishing that needs a build run,
which is out of scope here, so the requirement says the weaker and verified thing: these tests need only the beans to
exist, and they assert nothing that travels through a broker.

## Risks / Trade-offs

The audit stops being mechanical. Before this change, one grep for one annotation found every violation. After it,
finding a violation means reading what a test asserts. The enumerated type list bounds that work and makes it
greppable in practice, but a test that adds a broker assertion while keeping its embedded broker is now a violation
that no single grep reports. This is the cost of the decision and it is stated in the proposal rather than left to be
discovered later.

The narrowing could be read as blessing the five classes rather than as a rule about assertions. Mitigation: the
requirement states the boundary in terms of assertions and never names a file, so a sixth class gets the same test as
the first five.

An embedded broker in five `@SpringBootTest` contexts is still startup time spent on a broker that may not be needed.
That is now a performance question about five test classes rather than a specification violation, and it belongs to
whoever owns those tests.

## Migration Plan

Archive this change and confirm the merged specification carries the narrowed requirement, still forbids H2, still
pins `postgres:17-alpine`, and still carries the two corrections made earlier today in the requirements this delta
does not touch. Rollback is `git checkout` on the one merged specification, because nothing is built, deployed or
migrated.
