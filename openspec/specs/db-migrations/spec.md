# db-migrations Specification

## Purpose
TBD - created by archiving change db-migrations-flyway. Update Purpose after archive.
## Requirements
### Requirement: Per-service Flyway migrations own schema evolution
Each data-bearing service (sky-booking, sky-offer, sky-message) MUST own its database schema via Flyway migrations under `src/main/resources/db/migration/`. Hibernate `ddl-auto` MUST be set to `validate` in default and production profiles; schema changes go through versioned SQL migrations only.

#### Scenario: Applying a schema change in production
- **WHEN** a developer adds a new column to an entity in sky-offer and writes `V<n>__add_column.sql`
- **THEN** on next deploy, Flyway applies the migration before the service serves traffic; if the entity and schema disagree, `validate` mode fails startup loudly

#### Scenario: Running tests against a clean database
- **WHEN** the integration tests start a Testcontainers MySQL
- **THEN** Flyway applies all migrations from V1 onwards before the test runs; no manual SQL setup is required

### Requirement: No manual SQL setup steps in README
The README MUST NOT require human-run SQL files as a setup step. All schema and seed-data SQL MUST live under each service's `db/migration/` folder, applied automatically by Flyway.

#### Scenario: New developer onboarding
- **WHEN** a new developer follows the README "DB configuration" section
- **THEN** the steps are: create the database, start the services. Flyway handles everything else; no `mysql < sql_offers_insert.sql` step exists

