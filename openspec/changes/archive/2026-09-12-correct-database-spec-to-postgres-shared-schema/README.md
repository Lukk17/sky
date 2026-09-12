# correct-database-spec-to-postgres-shared-schema

Correct database-schemas and db-migrations to the shipped layout: one PostgreSQL `sky` database, all tables in `public`, and a Flyway history table per service.
