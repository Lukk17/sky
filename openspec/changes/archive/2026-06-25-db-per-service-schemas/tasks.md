## 1. Inventory the old schema

- [x] 1.1 Connect to current `sky` schema, list all tables: `SHOW TABLES`. Categorize each by owning service.
- [x] 1.2 Flag any unexpected tables (orphaned, undocumented). Decide for each: keep, drop, or assign to a service.
- [x] 1.3 Confirm row counts per table to verify the data-movement script later.

## 2. Schema-creation SQL

- [x] 2.1 Write `config/script/sql_commands/V0__create_per_service_schemas.sql` (or inline in Helm chart) that creates `sky_booking`, `sky_offer`, `sky_message` if not exists, with charset `utf8mb4`, collation `utf8mb4_unicode_ci`.
- [x] 2.2 Helm: update `config/k8s/helm/db/mysql/` values + init scripts to apply the three schemas on first start.

## 3. Data move (one-time)

- [x] 3.1 Write a SQL script per service that does `CREATE TABLE sky_<svc>.<tbl> LIKE sky.<tbl>; INSERT INTO sky_<svc>.<tbl> SELECT * FROM sky.<tbl>;` for each owned table. Save under `config/script/sql_commands/migrate_split_schemas.sql` for the apply pass.
- [x] 3.2 Verify row counts match between old and new tables.
- [x] 3.3 Verify `AUTO_INCREMENT` values are preserved (MySQL `CREATE TABLE ... LIKE` preserves table definition but not next-value; re-set via `ALTER TABLE ... AUTO_INCREMENT = N` if needed).

## 4. Reconfigure services

- [x] 4.1 Update `sky-booking/src/main/resources/application.yaml` JDBC URL to `${SPRING_DATASOURCE_URL:jdbc:mysql://host.docker.internal:3306/sky_booking}`. Same in `application-local.yaml`.
- [x] 4.2 Repeat for sky-offer and sky-message with their respective schemas.
- [x] 4.3 Update env-var defaults in `config/k8s/helm/service/*/values.yaml` for each service.
- [x] 4.4 Update `MYSQL_DATABASE_NAME` env var documentation in README.

## 5. Flyway adjustments

- [x] 5.1 Each service's Flyway now writes to its own schema's history table. If `db-migrations-flyway` shipped before this change, the existing history table in `sky` must be moved per-service (`INSERT INTO sky_booking.flyway_schema_history SELECT * FROM sky.flyway_schema_history WHERE ...`).
- [x] 5.2 Confirm `spring.flyway.schemas: sky_<svc>` set if needed (Flyway picks up from JDBC URL by default).

## 6. Cutover

- [x] 6.1 Plan: stop services → run data-move SQL → verify counts → start services pointing at new schemas → smoke-test booking/offer/message flows → if green, `DROP DATABASE sky`.
- [x] 6.2 In K8s, this is a brief restart per service; downtime measured in seconds.

## 7. Verify

- [x] 7.1 Integration tests pass against new schemas (in CI: Testcontainers per service spins up MySQL with the three schemas).
- [x] 7.2 `SHOW DATABASES` in prod shows only `sky_booking`, `sky_offer`, `sky_message` (and system DBs); `sky` is gone.
- [x] 7.3 Each service's `/actuator/health/db` reports UP.
- [x] 7.4 Bruno E2E run is green.
