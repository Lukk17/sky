## Why

`database-schemas` and `db-migrations` describe a database layout this repository does not have, and in the first case
close to the opposite of it. Both were written when the project ran MySQL with a schema per service, and both survived
the move to PostgreSQL unchanged, so a reader using them to judge whether a change regressed anything would reject the
shipped layout and accept the abandoned one.

`database-schemas` requires that `sky-booking`, `sky-offer` and `sky-message` each connect to a dedicated MySQL schema
named `sky_booking`, `sky_offer` or `sky_message`, and its first scenario asserts that the legacy `sky` schema does not
exist. Every one of those four facts is false. All three services point at one PostgreSQL database named `sky`:
`sky-booking/src/main/resources/application.yaml` line 61, `sky-offer/src/main/resources/application.yaml` line 76 and
`sky-message/src/main/resources/application.yaml` line 53 all default `spring.datasource.url` to
`jdbc:postgresql://host.docker.internal:5432/sky` with `driver-class-name: org.postgresql.Driver`. The strings
`sky_booking`, `sky_offer` and `sky_message` appear in no live source file, no chart and no compose file. The `sky`
database is the one that exists: `config/k8s/helm/db/postgres/values.yaml` line 12 sets `dbName: "sky"` and
`config/docker/docker-compose.ci.yaml` line 42 sets `POSTGRES_DB: sky`.

The isolation mechanism is therefore not the one the specification names. Across the eight migration files the five
`CREATE TABLE` statements are all unqualified, so every table lands in `public`, and no module sets a `default-schema`,
a `search_path` or a `currentSchema`. What keeps three services safely sharing that one schema is a Flyway history table per service, named
in each module's own configuration: `flyway_schema_history_booking` at
`sky-booking/src/main/resources/application.yaml` line 48, `flyway_schema_history_offer` at
`sky-offer/src/main/resources/application.yaml` line 63 and `flyway_schema_history_message` at
`sky-message/src/main/resources/application.yaml` line 44. That mechanism appears nowhere in either specification.

`db-migrations` is stale in two narrower ways. Its scenario on running tests against a clean database names a
Testcontainers MySQL, where all three modules pin `postgres:17-alpine`. And its second requirement claims all seed-data
SQL lives under `db/migration/`, where the demo seed is a repeatable migration at
`sky-<service>/src/main/resources/db/demo/R__demo_seed.sql`, reached only because
`application-local.yaml` widens `spring.flyway.locations` to `classpath:db/migration,classpath:db/demo`.

## What Changes

- Retire the `database-schemas` requirement that a service owns a dedicated MySQL schema, and replace it with one
  stating the layout that ships: one PostgreSQL database named `sky`, every table in `public`, ownership expressed as
  a service writing only its own tables, and cross-service reads still going through REST or Kafka.
- Rename the `database-schemas` Flyway requirement, which says the history table lives in the service's own schema, and
  restate it around the real mechanism, a Flyway history table per service inside the one shared schema, naming all
  three table names.
- Restate both `db-migrations` requirements: PostgreSQL 17 in place of MySQL in the Testcontainers scenario, the demo
  seed's real location under `db/demo/` and its local-profile-only activation in place of the claim that all seed SQL
  lives under `db/migration/`, and the MySQL-era `mysql < sql_offers_insert.sql` example replaced by what the
  onboarding steps actually are.
- Remove the semicolon clause joins from every requirement and scenario line being restated, which is all four in each
  file, because the repository formatting rule forbids them and these sentences are being rewritten from evidence
  anyway.

The cross-service access rule, the `ddl-auto: validate` rule, the Flyway-owns-evolution rule and the no-manual-SQL rule
all survive: they are true today and they are carried forward. Nothing here changes code, so nothing is built, migrated
or deployed.

## Capabilities

### New Capabilities

None. Both capabilities already exist.

### Modified Capabilities

- `database-schemas`: the per-service-schema requirement is retired and replaced, and the Flyway history requirement is
  renamed and restated around a per-service history table in one shared schema.
- `db-migrations`: the test-container engine in the first requirement, and the seed-data location and onboarding
  example in the second.

## Impact

- Affected files: `openspec/specs/database-schemas/spec.md` and `openspec/specs/db-migrations/spec.md`, rewritten at
  archive time from the delta specs in this change.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched. The evidence for every
  correction is a committed file that is being read rather than changed.
- Both files are corrected in one change because they describe one decision. A reader who learns from
  `database-schemas` that the three services share `public` needs `db-migrations` to say which engine the migrations
  run against, and splitting them would leave one half of the layout corrected and the other half contradicting it.
- Risk: low for the restated rules, moderate for the retirement. Retiring a requirement is the one operation here that
  loses a contract rather than repairing it, so the delta records the reason as a reversed decision and points at the
  requirement that replaces it, and the archived change `2026-06-25-db-per-service-schemas` remains the record of the
  layout that was abandoned.
