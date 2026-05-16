## Why

Three production-critical DB issues exist today:

1. **`spring.jpa.hibernate.ddl-auto: update` in every environment** (booking line 42, offer line 39, message line 35). Multiple replicas racing schema changes can corrupt the DB; silent column drops on entity renames are possible.
2. **No migration framework** (no Flyway, no Liquibase). The README documents *manually* running `sql_create_schema.sql`, `sql_offers_insert.sql`, `sql_messages_insert.sql`. Schema evolution is implicit, unversioned, and unrecoverable in case of drift.
3. **DDL is whatever Hibernate generates this minute**, which differs between Hibernate versions. Bumping Hibernate (which happens on every Spring Boot upgrade) can silently re-shape columns.

Flyway is the lightweight answer: SQL-first migrations under `db/migration/`, monotonically versioned, recorded in a `flyway_schema_history` table, ddl-auto set to `validate`. Adopt now, baseline from current schema, never write SQL by hand again.

## What Changes

- **Add** `org.flywaydb:flyway-core` and `org.flywaydb:flyway-mysql` to `sky-booking`, `sky-offer`, `sky-message`. Catalog-managed.
- **Capture** the current Hibernate-generated schema per service as the baseline. Run each service in a clean DB, dump the schema (`mysqldump --no-data`), commit as `V1__init.sql` per service. Add the contents of `sql_offers_insert.sql` and `sql_messages_insert.sql` as separate seed migrations (e.g., `R__seed_test_data.sql`) under a `dev` profile.
- **Set** `spring.flyway.enabled: true`, `spring.flyway.locations: classpath:db/migration` in each service's `application.yml`.
- **Flip** `spring.jpa.hibernate.ddl-auto: validate` in `application.yml` (default profile = prod-like). Keep `update` only under `application-local.yml` if a developer profile explicitly opts in (or set to `validate` there too and require migrations).
- **Delete** `config/script/sql_commands/sql_create_schema.sql` (Flyway creates schemas if `schemas:` is set). Delete the seed SQLs after promoting them into migrations.
- **Update** README's "DB configuration" section to say "start services; Flyway applies migrations; no manual SQL."

## Capabilities

### New Capabilities
- `db-migrations`: Each data-bearing service owns its own `db/migration/` directory; `flyway_schema_history` tracks applied migrations; `ddl-auto: validate` enforces schema-entity alignment at startup.

### Modified Capabilities
- _None._ Schema is unchanged; only the *mechanism* by which it arrives changes.

## Impact

- **Touched files**: 3 × `build.gradle.kts` (flyway deps), 3 × `application.yml` (flyway config + ddl-auto change), 3 × `application-local.yml` if profile-specific overrides needed, 3 new `db/migration/V1__init.sql` files, 1 README update, deletion of 3 SQL scripts under `config/script/`.
- **Existing local DBs**: developers must either start with a clean schema or `flyway baseline` against their existing one. Documented in tasks and README.
- **Existing prod DB**: same — `flyway baseline` once, with `baselineVersion: 1` so V1 is treated as already applied.
- **CI**: tests use H2 (or Testcontainers MySQL after `test-modernization`) — both flyway-compatible.
- **Risk**: medium. Flyway baseline ceremony has to be right. Once done, ongoing risk drops to near zero.
- **Dependency order**: should land before `db-per-service-schemas` so each service already has a migration framework when its schema splits off. Independent of Spring Boot 4 work.
