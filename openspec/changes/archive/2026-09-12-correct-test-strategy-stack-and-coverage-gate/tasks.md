## 1. Establish the container stack

- [x] 1.1 Record the pinned PostgreSQL image by reading `TestcontainersConfiguration` in `sky-booking`, `sky-offer` and
  `sky-message`, and verify all three parse `postgres:17-alpine`
- [x] 1.2 Confirm MySQL appears in no module test tree by grepping the five module source trees case-insensitively and
  verifying zero matches
- [x] 1.3 Confirm H2 is gone by grepping the five module source trees and `gradle/libs.versions.toml` for `org.h2` and
  `h2database` and verifying zero matches
- [x] 1.4 Confirm which modules need which container by reading the `plugins` block of each module build file and
  verifying `sky-message` does not apply `sky.kafka-conventions` and `sky-notify` does not apply `sky.web-conventions`,
  and that `sky-notify` declares no JDBC driver
- [x] 1.5 Confirm the Kafka container type by grepping the test trees for `ConfluentKafkaContainer` and recording which
  classes bring one up

## 2. Establish the EmbeddedKafka divergence

- [x] 2.1 List every class that declares `@EmbeddedKafka` by grepping the five module test trees, and verify the set is
  exactly the five named in design.md
- [x] 2.2 For each of those five, verify it is a `@SpringBootTest` and that it imports its module's
  `TestcontainersConfiguration`, so the divergence is about the broker and not about the database
- [x] 2.3 For each of those five, verify it references no `KafkaTemplate`, no Kafka consumer and no `ConsumerRecord`, so
  the claim that none of them exercises Kafka rests on a grep rather than on an impression
- [x] 2.4 Verify the tests that do exercise Kafka use a container, by listing the classes that extend each module's
  `AbstractIntegrationTest` and confirming that class brings up a `ConfluentKafkaContainer`
- [x] 2.5 Decide and record the handling: keep the prohibition, do not weaken the requirement, and report the five
  paths with the evidence from 2.3

## 3. Establish the coverage gate

- [x] 3.1 Record the two minima and the counters by reading the `violationRules` block of
  `buildSrc/src/main/kotlin/sky.jacoco-conventions.gradle.kts`, and verify LINE and BRANCH are both `0.90`
- [x] 3.2 Record the wiring point by reading the same file and verifying the `dependsOn` is on `check`, not on `build`
- [x] 3.3 Record the exclusion list from the same file and verify it is `**/dto/**`, `**/config/**`,
  `**/*Application.class` and `**/Constants.class`
- [x] 3.4 Confirm the violation rule sets no `element`, so the check is evaluated at the default bundle scope and the
  scenario must not promise a per-package breakdown
- [x] 3.5 Identify every module that disables the verification task by grepping every module build file for
  `jacocoTestCoverageVerification`, and verify `sky-gateway` is the only one
- [x] 3.6 Confirm the `sky-gateway` opt-out is structural by listing its main source tree and verifying its only two
  classes are an application class and a class under `config/`, both of which the exclusions in 3.3 remove

## 4. Write the delta specification

- [x] 4.1 Read the current `openspec/specs/test-strategy/spec.md` rather than assuming its content, because the change
  archived as `2026-09-12-correct-stale-spec-requirements` modified it today
- [x] 4.2 Verify the requirement `Authenticated and unauthenticated paths are both tested` is absent from this delta, so
  that earlier correction cannot be undone
- [x] 4.3 Write `specs/test-strategy/spec.md` under `## MODIFIED Requirements`, carrying both whole requirement blocks,
  and verify both requirement headers and both existing scenario names match the merged file character for character
- [x] 4.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against a
  fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 4.5 Verify the delta respects the wrap width of the merged file, which is one physical line per paragraph and per
  scenario bullet
- [x] 4.6 Run `openspec validate correct-test-strategy-stack-and-coverage-gate --strict` and verify it reports no error

## 5. Archive and sync

- [x] 5.1 Archive the change and verify `openspec/specs/test-strategy/spec.md` contains no `MySQL`, no `80%` and no
  `70%`
- [x] 5.2 Verify the same file now contains `postgres:17-alpine`, the phrase 0.90 line coverage and 0.90 branch
  coverage, and the phrase as part of check
- [x] 5.3 Verify the same file still contains the status set from the earlier correction, by checking that the 409 and
  the 503 lines and the `POST /api/v1/bookings` path are all still present
- [x] 5.4 Verify the `@EmbeddedKafka` prohibition survived the restatement and was not softened
- [x] 5.5 Verify the change directory moved under `openspec/changes/archive/` and that `openspec list` no longer reports
  it active
- [x] 5.6 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 6. Notes from the run

- The merged-file rewrite was performed by `openspec archive correct-test-strategy-stack-and-coverage-gate --yes`, which
  reported `~ 2 modified` against `test-strategy` and touched nothing else in the file. No hand edit of a merged
  specification was needed.
- Task 5.3 confirmed the correction made earlier today by `2026-09-12-correct-stale-spec-requirements` is intact: the
  409 line, the 503 line and the `POST /api/v1/bookings` path are all still present in the requirement this change did
  not touch.
- Task 2.5 stands as the open item this change deliberately did not close. The five classes from task 2.1 still declare
  `@EmbeddedKafka` and the requirement still forbids it. Task 2.3 confirmed none of the five references a
  `KafkaTemplate`, a Kafka consumer or a `ConsumerRecord`, so the divergence costs no coverage today, but it is a real
  failing contract and it needs an owner. The two routes are named in design.md, Decisions.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. Left as
  written, for the same reason as the two changes archived before it today: each corrected number needs the file and
  line it was read from.
