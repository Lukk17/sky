## Why

Two `test-strategy` requirements describe a test stack and a coverage gate this repository no longer has, and the
coverage one understates the bar by ten and twenty points. A contributor reading it would aim at 80 percent line and 70
percent branch coverage and find the build failing at 0.90 on both, and would look for a MySQL container that no module
starts.

The container engine is PostgreSQL. All three data-bearing modules pin the image in their own
`TestcontainersConfiguration`: `sky-booking/src/test/java/com/lukk/sky/booking/TestcontainersConfiguration.java` line
23, `sky-offer/src/test/java/com/lukk/sky/offer/TestcontainersConfiguration.java` line 20 and
`sky-message/src/test/java/com/lukk/sky/message/TestcontainersConfiguration.java` line 17 each parse
`postgres:17-alpine`. No module references MySQL anywhere.

The coverage gate is `jacocoTestCoverageVerification` in
`buildSrc/src/main/kotlin/sky.jacoco-conventions.gradle.kts`, which sets a LINE minimum of `0.90` at line 45 and a
BRANCH minimum of `0.90` at line 49, and wires itself into `check` at line 56 rather than into `build`. The same file
filters the measured set at lines 7 to 12, excluding `**/dto/**`, `**/config/**`, `**/*Application.class` and
`**/Constants.class`. One module opts out: `sky-gateway/build.gradle.kts` lines 12 to 14 disable the task, and the
reason is structural rather than a concession, because the module's only two classes are
`sky-gateway/src/main/java/com/lukk/sky/gateway/SkyGatewayApplication.java` and
`sky-gateway/src/main/java/com/lukk/sky/gateway/config/SecurityConfig.java`, so the two exclusions above leave nothing
to measure. The specification mentions none of this, and its scenario promises a per-package breakdown on failure that
the rule cannot produce: the violation rule sets no `element`, so it is evaluated over the whole module bundle.

## What Changes

- Restate the Testcontainers requirement so it names PostgreSQL with the pinned `postgres:17-alpine` image, and so it
  says which containers each module actually needs, because `sky-message` has no Kafka dependency at all and
  `sky-notify` has no datastore.
- Keep that requirement's prohibition on an in-memory database and on `@EmbeddedKafka` exactly as strict as it is
  today. This is the one place in the three specifications being corrected where the contract is ahead of the code
  rather than behind it, and five test classes currently break it. They are reported rather than blessed, and the
  requirement is not weakened to accommodate them.
- Restate the coverage requirement with the real numbers, 0.90 line and 0.90 branch, the real gate task and the real
  wiring point, `check`, the real exclusion list, and the rule that a module whose measured set the exclusions empty
  disables the task in its own build file instead of the floor being lowered for everyone.
- Correct that requirement's scenario, which promises a per-package breakdown. The violation rule names no `element`,
  so the check is evaluated and reported over the module bundle.
- Add one scenario covering the empty-measured-set opt-out, so the `sky-gateway` exception is a checkable rule rather
  than an unexplained disabled task.

Nothing here changes code. The two requirements are aligned to a build file and three test configurations that already
ship, so no test and no Gradle task moves.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `test-strategy`: the container engine in the Testcontainers requirement, and the thresholds, gate task, wiring point,
  exclusion list and failure reporting in the coverage requirement.

## Impact

- Affected file: `openspec/specs/test-strategy/spec.md`, rewritten at archive time from the delta spec in this change.
  Only the two requirements named above are touched.
- The requirement `Authenticated and unauthenticated paths are both tested` in the same file was corrected by the change
  archived as `2026-09-12-correct-stale-spec-requirements` and is deliberately left alone, so that correction is not
  undone. The two requirements not named here, on negative-path tests and on end-to-end runs, were read and are not
  restated, because this change has no evidence pass behind them.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched, and no build is run: every
  number comes from reading a committed build file rather than from a run.
- Risk: low for the corrections, which raise the documented bar to the enforced one. The one open item is the
  `@EmbeddedKafka` divergence, which this change deliberately leaves as a failing contract rather than resolving it in
  either direction, because resolving it means either editing test code or relaxing a rule, and neither belongs in a
  specification correction.
