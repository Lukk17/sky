## 1. Establish the engine and the database

- [x] 1.1 Confirm the engine and the database name by reading the `spring.datasource` block in
  `sky-booking/src/main/resources/application.yaml`, `sky-offer/src/main/resources/application.yaml` and
  `sky-message/src/main/resources/application.yaml`, and verifying all three default to
  `jdbc:postgresql://host.docker.internal:5432/sky` with `driver-class-name: org.postgresql.Driver`
- [x] 1.2 Confirm the schemas the specification names exist nowhere by grepping the whole working tree for
  `sky_booking`, `sky_offer` and `sky_message` and verifying every hit is in `openspec/` or in untracked `.idea/`
  history, with none in a module source tree, a chart or a compose file
- [x] 1.3 Confirm MySQL is gone by grepping the whole working tree case-insensitively for `mysql` and verifying no hit
  is in a module source tree, a chart or a compose file
- [x] 1.4 Confirm the deployed and local database names by reading `config/k8s/helm/db/postgres/values.yaml` and
  `config/docker/docker-compose.ci.yaml` and verifying `dbName: "sky"` and `POSTGRES_DB: sky`

## 2. Establish the schema and the history mechanism

- [x] 2.1 Confirm every table lands in `public` by reading all eight files under the three
  `src/main/resources/db/migration/` directories and verifying every `CREATE TABLE` is unqualified
- [x] 2.2 Confirm nothing redirects the schema by grepping the module source trees for `default-schema`,
  `defaultSchema`, `search_path`, `currentSchema` and `schemas:` and verifying zero matches
- [x] 2.3 Record the three history table names by reading `spring.flyway.table` in each of the three
  `application.yaml` files, and verify they are `flyway_schema_history_booking`, `flyway_schema_history_offer` and
  `flyway_schema_history_message`
- [x] 2.4 Confirm `ddl-auto` is `validate` in all three modules' default profile, so that surviving rule is carried
  forward on evidence rather than on trust

## 3. Establish the test engine and the seed location

- [x] 3.1 Confirm the test container image by reading `TestcontainersConfiguration` in all three data-bearing modules
  and verifying each parses `postgres:17-alpine`
- [x] 3.2 Confirm the demo seed location by finding every `.sql` file outside `db/migration/` and verifying the only
  ones are `sky-<service>/src/main/resources/db/demo/R__demo_seed.sql` for the three data-bearing services
- [x] 3.3 Confirm the seed is local-profile only by reading `spring.flyway.locations` in each
  `application-local.yaml` and verifying it is `classpath:db/migration,classpath:db/demo`, and that the default
  profile's value is `classpath:db/migration` alone
- [x] 3.4 Confirm no document asks a human to run SQL by grepping every markdown file outside `openspec/` and
  `.agents/` for `.sql` and verifying no hit is an instruction to execute one

## 4. Write the delta specifications

- [x] 4.1 Write `specs/database-schemas/spec.md` with a `## RENAMED Requirements` pair for the Flyway history
  requirement, a `## REMOVED Requirements` block carrying `**Reason**` and `**Migration**` for the per-service schema
  requirement, a `## MODIFIED Requirements` block under the renamed header, and an `## ADDED Requirements` block for the
  replacement, and verify the RENAMED FROM header and the REMOVED header each match the merged file character for
  character
- [x] 4.2 Verify the MODIFIED block in `specs/database-schemas/spec.md` carries the scenario name
  `Inspecting migration state` unchanged, because the archive step refuses a MODIFIED block that drops a scenario name
- [x] 4.3 Write `specs/db-migrations/spec.md` under `## MODIFIED Requirements`, carrying both whole requirement blocks,
  and verify both requirement headers and all three scenario names match the merged file character for character
- [x] 4.4 Verify both deltas hold no em dash, no en dash, no semicolon joining two clauses, and no bold or italic
  outside the `**WHEN**`, `**THEN**`, `**Reason**` and `**Migration**` markers the format requires, using a byte-exact
  matcher first validated against a fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 4.5 Verify both deltas respect the wrap width of the merged files they target, which is one physical line per
  paragraph and per scenario bullet in both
- [x] 4.6 Run `openspec validate correct-database-spec-to-postgres-shared-schema --strict` and verify it reports no
  error

## 5. Archive and sync

- [x] 5.1 Archive the change and verify `openspec/specs/database-schemas/spec.md` contains no `MySQL`, no `sky_booking`,
  no `sky_offer` and no `sky_message`, and now names all three Flyway history tables
- [x] 5.2 Verify `openspec/specs/db-migrations/spec.md` contains no `MySQL` and no `mysql <`, and now names
  `postgres:17-alpine` and `db/demo/R__demo_seed.sql`
- [x] 5.3 Verify both merged specifications still carry their surviving rules: cross-service access over REST or Kafka,
  `ddl-auto: validate`, Flyway owning evolution, and no manual SQL step
- [x] 5.4 Verify the change directory moved under `openspec/changes/archive/` and that `openspec list` no longer reports
  it active
- [x] 5.5 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 6. Notes from the run

- The merged-file rewrite was performed by `openspec archive correct-database-spec-to-postgres-shared-schema --yes`,
  which reported `+ 1 added, ~ 1 modified, - 1 removed, → 1 renamed` against `database-schemas` and `~ 2 modified`
  against `db-migrations`. All four delta verbs applied in one pass, confirming the RENAMED then REMOVED then MODIFIED
  then ADDED order read out of `specs-apply.js`. No hand edit of a merged specification was needed.
- Task 5.1 reads one remaining match for `sky_booking`, `sky_offer` and `sky_message` in
  `openspec/specs/database-schemas/spec.md`. It is the deliberate negative assertion in the scenario
  `Verifying the deployed database layout`, which states that no such database or schema exists. There is no remaining
  match for `MySQL`.
- Archive placed the renamed Flyway history requirement above the replacement requirement, because the removed
  requirement vacated the first slot and an ADDED requirement appends at the tail. The merged file therefore introduces
  the history table before the layout it sits in. Left as archive produced it: reordering would mean editing a merged
  specification directly, which is the route this change exists to avoid.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. Left as
  written: the three false facts in `database-schemas` each need their evidence named, and the archived change
  `2026-09-12-correct-stale-spec-requirements` sets the precedent for a Why of that length.
