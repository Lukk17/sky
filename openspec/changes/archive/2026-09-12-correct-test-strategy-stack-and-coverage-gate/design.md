## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec sync, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops a
scenario name the merged requirement has. Both requirements restated here keep their existing scenario names unchanged,
and the one new scenario is an addition inside a restated block rather than a rename.

This file was already corrected once today. The change archived as `2026-09-12-correct-stale-spec-requirements` rewrote
the requirement `Authenticated and unauthenticated paths are both tested`, replacing a placeholder status pair with the
full set its handlers return. That requirement is not in this delta, so that correction stands.

## Goals / Non-Goals

**Goals:**

- State the coverage floor that is actually enforced, because a documented floor below the enforced one is worse than no
  documented floor: it tells a contributor the build will pass when it will fail.
- Name the engine and the pinned image tag, so the specification goes stale loudly on the next engine bump instead of
  silently, which is what happened to the MySQL version of this requirement.
- Record the `sky-gateway` opt-out as a rule with a stated condition, so the next module with an empty measured set has
  a sanctioned route and nobody reads a disabled task as permission to disable it anywhere.

**Non-Goals:**

- Relaxing the prohibition on `@EmbeddedKafka`. See Decisions.
- The two requirements in this file not restated here, `Every public method has a negative-path test` and
  `E2E tests run against a freshly-deployed stack`. Both were read, neither was checked against the code in the detail
  a restatement obliges, and restating a block carries an obligation to correct every stale detail inside it. Claiming
  that obligation was met without the evidence pass is the failure this whole line of work exists to stop.
- Any code change, including the five test classes reported below.

## Decisions

Keep the `@EmbeddedKafka` prohibition, and report the code that breaks it. This is the one requirement across the three
specifications being corrected where the contract is ahead of the implementation, so correcting the specification toward
the code would delete a rule rather than repair a description. Five `@SpringBootTest` classes still declare
`@EmbeddedKafka` while importing a Testcontainers PostgreSQL configuration:
`sky-booking/src/test/java/com/lukk/sky/booking/adapters/inbound/api/BookingControllerTest.java`,
`sky-booking/src/test/java/com/lukk/sky/booking/SkyBookingApplicationTests.java`,
`sky-offer/src/test/java/com/lukk/sky/offer/adapters/inbound/api/OfferApiControllerTest.java`,
`sky-offer/src/test/java/com/lukk/sky/offer/adapters/inbound/api/OfferApiDocumentTest.java` and
`sky-offer/src/test/java/com/lukk/sky/offer/DemoSeedMigrationTest.java`.

The mitigating detail, which is why this is a divergence rather than a defect with a user-visible consequence: none of
the five touches Kafka. Each mocks the notification port, none references a `KafkaTemplate`, a consumer or a
`ConsumerRecord`, and the embedded broker exists only so the Spring Kafka beans can be created while the context starts.
The tests that genuinely exercise Kafka extend each module's `AbstractIntegrationTest`, which brings up a
`ConfluentKafkaContainer`. So the spirit of the requirement holds and its letter does not.

That leaves a real choice, and it is not this change's to make. Resolving it means either replacing the embedded broker
in five classes with a container, which slows five context-level tests for no assertion gained, or narrowing the rule to
bind only on tests that assert through a broker, which is a decision about the test strategy rather than a correction
to a stale fact. Both are code or policy decisions with an owner, so the requirement is left strict and the divergence
is reported.

Say `check` rather than `build`. The old text named `./gradlew build`, which is not false, because `build` depends on
`check`, but it points a reader at the wrong file when they go looking for the wiring. The `dependsOn` is on `check` in
`buildSrc/src/main/kotlin/sky.jacoco-conventions.gradle.kts`, and naming it means `./gradlew check` is also understood
to gate, which it does.

Describe the exclusion set in behaviour terms rather than as four glob patterns. Wire DTOs, configuration classes, the
application class and constants holders is what the globs mean, and a specification that pins the glob syntax goes stale
on a package rename that does not change the contract at all.

State the failure report as what the rule can produce. The old scenario promised a per-package breakdown. The violation
rule in the convention plugin declares two limits and no `element`, so it is evaluated at the default bundle scope and
the message names the counter, the ratio and the minimum for the module. This was established by reading the build file
rather than by running the gate, because a build run was out of scope for this change.

## Risks / Trade-offs

Leaving a requirement that the code breaks means `test-strategy` now knowingly fails against five files. That is a real
cost: anyone auditing against it will find the same five and may assume the specification is stale again. Mitigation:
the divergence is stated here with all five paths and with the evidence that none of them exercises Kafka, so the next
reader inherits the analysis rather than redoing it.

Pinning `postgres:17-alpine` in a specification duplicates a value that lives in three test classes, so a bump has to
touch both. That is deliberate: the previous version of this requirement named an engine rather than a version and
survived an entire engine migration without anyone noticing, which is the more expensive failure.

The bundle-scope claim about the failure message is derived from the absence of an `element` setting, not from an
observed failing run. If the reported scope ever matters to a reader, it is one line in the convention plugin to
re-check.

## Migration Plan

Archive this change and confirm the merged specification carries the corrected blocks and still carries the status-set
correction made earlier today. Rollback is `git checkout` on the one merged specification, because nothing is built,
deployed or migrated.
