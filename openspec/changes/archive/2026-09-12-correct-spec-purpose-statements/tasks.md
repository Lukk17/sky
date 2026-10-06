## 1. Establish which Purpose statements drifted

- [x] 1.1 Read the Purpose of each capability touched this evening, `api-versioning`, `spring-boot-hygiene` and
  `architecture`, against the requirement list directly below it, and record for each whether it still agrees
- [x] 1.2 Confirm `api-versioning` needs no change, by checking that its Purpose makes no claim about how the version is
  carried and none about an internal namespace
- [x] 1.3 Record the exact contradiction in `spring-boot-hygiene`, which is that its Purpose says no credential is
  committed as a default while its credential requirement now exempts the `local` profile and three files use that
  exemption
- [x] 1.4 Record the two contradictions in `architecture`, which are that its Purpose says every service while the
  layout requirement names `sky-gateway` and `sky-common` as outside it, and that it says the package structure is
  checked by a test while the requirement names exactly three locations an ArchUnit rule asserts
- [x] 1.5 Confirm no other merged specification's Purpose was contradicted by this evening's three changes, by checking
  the Purpose of every capability whose requirements were not touched and verifying none of them references a rule that
  moved

## 2. Rewrite the two Purpose statements

- [x] 2.1 Replace the Purpose of `openspec/specs/spring-boot-hygiene/spec.md` so it names the local-profile exemption
  and the wildcard prohibition, keeping it to one sentence and to the one physical line the file uses
- [x] 2.2 Replace the Purpose of `openspec/specs/architecture/spec.md` so it names the four hexagonal services and says
  each service's own ArchUnit test asserts the parts a build can check, keeping it to one physical line
- [x] 2.3 Verify each new Purpose is at least fifty characters, which is what `openspec validate --strict` accepts, and
  that neither reintroduces a `TBD` placeholder
- [x] 2.4 Verify neither Purpose holds an em dash, an en dash, a semicolon joining two clauses, or bold or italic, using
  a byte-exact matcher first validated against a fixture containing an em dash, an en dash, an arrow, a bullet and a
  box-drawing character
- [x] 2.5 Verify `git diff openspec/specs/` shows exactly one changed line per file beyond what the three archived
  requirement changes already changed, and that the requirement and scenario counts of both files are unchanged

## 3. Validate and archive

- [x] 3.1 Run `openspec validate --specs --strict` across all eighteen specifications and verify it reports eighteen
  passed and none failed
- [x] 3.2 Archive the change and verify the archive reports no spec sync, since `skip_specs: true` means there is no
  delta to apply, and confirm the change tree landed under `openspec/changes/archive/`
- [x] 3.3 Verify `openspec/changes` holds nothing but `archive` and that `openspec list` reports no active change
- [x] 3.4 Verify `git status` shows no file outside `openspec/changes` and `openspec/specs` that this change touched

## 4. Notes from the run

- Task 1.5 holds. All eighteen Purpose statements were read against the corrections made this evening, and none of the
  sixteen untouched capabilities references a rule that moved. The three runbook capabilities wrap their Purpose over
  two physical lines at 120 columns, unlike the other fifteen, which is pre-existing and was left alone.
- Task 2.5 holds exactly. `git diff openspec/specs/` shows one changed Purpose line in each of the two files and nothing
  else beyond what the three archived requirement changes already changed. Requirement and scenario counts are unchanged
  at three and five for `spring-boot-hygiene` and at two and five for `architecture`.
- Task 3.2 confirmed the `skip_specs` path: the archive step reported no spec sync and no delta, and moved the change
  tree into the archive, which is the whole effect of archiving a change of this kind.
- Task 3.4 holds for this change. The working tree also carries another agent's in-flight Kafka producer work in
  `sky-booking`, `sky-offer` and `sky-notify`, which is not from here and was not touched.
- Nothing was committed. Every change in this evening's pass is left in the working tree for the owner to review and
  commit.
