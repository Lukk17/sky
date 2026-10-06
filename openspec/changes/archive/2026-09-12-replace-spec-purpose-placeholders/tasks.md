## 1. Establish the scope

- [x] 1.1 Confirm the placeholder is present in all eighteen specifications by printing the Purpose line of every
  `openspec/specs/*/spec.md` and verifying each one reads `TBD - created by archiving change ...`
- [x] 1.2 Record the requirement headers of each capability, so every Purpose is written from the contract in force
  rather than from the archived change that created it
- [x] 1.3 Record, per file, the current requirement count and the current longest line, to compare against after the
  rewrite

## 2. Rewrite the Purpose sections

- [x] 2.1 Replace the Purpose of the four build and platform capabilities (`gradle-build`, `framework-version`,
  `docker-build`, `kubernetes-deployment`) and verify each reads as one or two sentences naming what it governs
- [x] 2.2 Replace the Purpose of the three data capabilities (`database-schemas`, `db-migrations`, `kafka-messaging`)
  and verify none of them names a database engine or a broker version, since the engine already changed once
- [x] 2.3 Replace the Purpose of the four API and architecture capabilities (`api-versioning`, `architecture`,
  `sky-common`, `websocket`) and verify each distinguishes itself from its neighbours
- [x] 2.4 Replace the Purpose of the three hygiene capabilities (`repo-hygiene`, `spring-boot-hygiene`, `helm-charts`)
  and verify each states the concern rather than listing its own requirements
- [x] 2.5 Replace the Purpose of the four test capabilities (`test-strategy`, `booking-flow-e2e`, `messaging-e2e`,
  `offer-crud-e2e`) and verify the three e2e ones are distinguishable from each other by the flow each covers

## 3. Verify

- [x] 3.1 Verify no `openspec/specs/*/spec.md` still contains the string `TBD` or `Update Purpose after archive`
- [x] 3.2 Verify every Purpose is at least fifty characters, which is what `openspec validate --strict` accepts
- [x] 3.3 Verify `git diff openspec/specs/` changes only lines inside a `## Purpose` section, and that the requirement
  count per file matches what task 1.3 recorded
- [x] 3.4 Verify no Purpose holds an em dash, an en dash, a semicolon joining two clauses, or bold or italic, using a
  byte-exact matcher first validated against a fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 3.5 Run `openspec validate --specs --strict` across all eighteen specifications and verify it reports no error
- [x] 3.6 Verify `git status` shows exactly the eighteen specifications as modified and no file outside
  `openspec/specs` and `openspec/changes`

## 4. Archive

- [x] 4.1 Archive the change and verify the archive reports no spec sync, since `skip_specs: true` means there is no
  delta to apply, and confirm the change tree landed under `openspec/changes/archive/`

## 5. Notes from the run

- Task 3.3 holds per file with one accounted-for exception. Sixteen specifications show exactly the Purpose lines
  changed (one replaced line, or two in the three runbook capabilities that wrap at 120 columns).
  `openspec/specs/offer-crud-e2e/spec.md` and `openspec/specs/test-strategy/spec.md` show additional changed lines
  because `correct-stale-spec-requirements` is archived but not committed, so its requirement corrections are still in
  the working tree. Those lines belong to that change and are recorded in its delta.
- Requirement and scenario counts are unchanged in all eighteen files. The only wrap-width movement is
  `messaging-e2e`, whose longest line went from 115 to 116 columns, inside the 120-column budget the three runbook
  capabilities already use.
- Semicolons joining two clauses are widespread in the pre-existing requirement and scenario text of eleven
  specifications. None was introduced here and none is in a Purpose section. Reported rather than fixed, because
  requirement text was not in scope.
