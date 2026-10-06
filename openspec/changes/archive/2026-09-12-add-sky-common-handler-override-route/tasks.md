## 1. Establish the override route

- [x] 1.1 Read `sky-common/src/main/java/com/lukk/sky/common/web/RestExceptionHandlerAutoConfiguration.java` and verify
  the `SkyRestExceptionHandler` bean carries `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`
- [x] 1.2 Verify in the same file that the `SpringDataExceptionHandler` bean carries
  `@ConditionalOnMissingBean(SpringDataExceptionHandler.class)`, so the override route is the same for both handlers
- [x] 1.3 Verify no service extends either shared handler, by grepping the four service source trees for
  `SkyRestExceptionHandler` and `SpringDataExceptionHandler` and confirming zero matches
- [x] 1.4 Verify the additive case is what the services actually do, by confirming each REST service's
  `GlobalExceptionHandler` is an independent `@RestControllerAdvice` that extends nothing from `sky-common`, so it adds
  handling rather than replacing the shared bean

## 2. Write the delta specification

- [x] 2.1 Write `specs/sky-common/spec.md` under `## MODIFIED Requirements`, carrying the whole requirement block, and
  verify the requirement header matches the merged file character for character
- [x] 2.2 Extract the four existing scenarios from `openspec/specs/sky-common/spec.md` and from the delta and compare
  them body for body, verifying all four are byte-identical and none is missing from the delta
- [x] 2.3 Verify the only prose difference from the merged requirement is the handler clause, so nothing else was
  improved while the stated scope was one clause
- [x] 2.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers, using a byte-exact matcher first validated against a fixture containing an em
  dash, an en dash, an arrow and a bullet
- [x] 2.5 Verify the delta respects the wrap width of the merged file, which is one physical line per paragraph and per
  scenario bullet
- [x] 2.6 Run `openspec validate add-sky-common-handler-override-route --strict` and verify it reports no error

## 3. Archive and sync

- [x] 3.1 Archive the change and verify `openspec/specs/sky-common/spec.md` now names `@ConditionalOnMissingBean` and the
  rule that a service replaces a shared handler by declaring its own bean of that type
- [x] 3.2 Re-run the check that task 5.2 of `2026-09-12-correct-sky-common-module-shape` failed, and verify all three of
  its items now pass: the `compileOnly` rule, the `@ConditionalOnMissingBean` override route, and all six packages
- [x] 3.3 Verify the four carried scenarios survived the archive unchanged, by comparing them against the copies taken
  before it
- [x] 3.4 Verify the change directory moved under `openspec/changes/archive/`, that `openspec list` reports no active
  change, and that `openspec/changes` holds nothing but `archive`
- [x] 3.5 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 4. Notes from the run

- The merged-file rewrite was performed by `openspec archive add-sky-common-handler-override-route --yes`, which reported
  `~ 1 modified` against `sky-common`. No hand edit of a merged specification was needed.
- Task 2.3 was checked with a sentence-level comparison of the requirement body against the merged file taken before the
  archive. Four sentences were kept verbatim, two were added, and none was removed or reworded, so the one-clause scope
  held.
- Task 3.2 confirms the gap this change exists to close is closed: all three items of task 5.2 of
  `2026-09-12-correct-sky-common-module-shape` now pass against the merged specification. That task stays unchecked in
  its own change, because it records what that change did, not what a later one fixed.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. Left as
  written, consistent with the four changes archived before it today.
