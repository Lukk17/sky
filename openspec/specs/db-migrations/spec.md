# db-migrations Specification

## Purpose
Makes versioned migrations the only way a service's schema changes, so a fresh checkout reaches a working database by starting the application rather than by following manual SQL instructions in a readme.

## Requirements

### Requirement: Per-service Flyway migrations own schema evolution
Each data-bearing service (sky-booking, sky-offer, sky-message) MUST own the tables it creates via Flyway migrations under `src/main/resources/db/migration/`, recorded in its own history table. Hibernate `ddl-auto` MUST be set to `validate` in default and production profiles, so a schema change reaches the database only as a versioned SQL migration and never as something Hibernate generates at startup.

#### Scenario: Applying a schema change in production
- **WHEN** a developer adds a new column to an entity in sky-offer and writes `V<n>__add_column.sql`
- **THEN** on the next deploy Flyway applies the migration before the service serves traffic, and if the entity and the database still disagree afterwards, `validate` mode fails startup loudly rather than altering anything

#### Scenario: Running tests against a clean database
- **WHEN** the integration tests start a Testcontainers PostgreSQL container, which every data-bearing module pins to `postgres:17-alpine`
- **THEN** Flyway applies all migrations from V1 onwards before the test runs, and no manual SQL setup is required

### Requirement: No manual SQL setup steps in README
No README may require a human to run a SQL file as a setup step. Schema SQL MUST live under each service's `db/migration/` folder and be applied automatically by Flyway. Seed data MUST also be Flyway-applied rather than hand-run, and it MUST be kept out of `db/migration/` so it cannot reach a deployed environment: the demo seed is the repeatable migration `db/demo/R__demo_seed.sql`, and only the `local` profile widens `spring.flyway.locations` to include `classpath:db/demo`.

#### Scenario: New developer onboarding
- **WHEN** a new developer follows the README database section
- **THEN** the steps are to create the `sky` database and start the services, Flyway handles schema and demo seed, and no step pipes a SQL file into a database client
