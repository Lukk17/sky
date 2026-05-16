## Context

"Each service owns its data" is the most quoted microservices principle, and the most violated in practice. The Sky audit found shared schema in name only — no JPA cross-service joins, no shared write paths, just a shared namespace. Splitting is more about cultural hygiene and operational clarity than fixing a runtime bug. But the value is real:

- A schema change in sky-offer literally cannot touch sky-booking's tables once they live in different schemas.
- Per-schema Flyway history tables don't compete for migration ordering.
- A `mysqldump sky_offer` snapshots one service's data, not the whole org.
- A future "extract sky-message to its own DB instance" becomes a `MYSQL_HOST` env var change rather than a schema split.

## Goals / Non-Goals

**Goals:**
- Three logical schemas on one MySQL instance: `sky_booking`, `sky_offer`, `sky_message`.
- Each service connects only to its own schema (MySQL user with grants only on its schema, ideally).
- No data loss; row counts pre/post move are identical.

**Non-Goals:**
- Splitting into separate MySQL *instances*. One instance is fine for this workload.
- Renaming or restructuring any table.
- Adopting per-service MySQL users with restricted grants. Worth doing eventually; out of scope here.
- Cross-service data replication / CDC. The REST + Kafka boundary already handles cross-service reads.

## Decisions

1. **`CREATE TABLE LIKE` + `INSERT SELECT`**, not `RENAME TABLE`. Reason: `RENAME` across schemas works in MySQL but locks both tables; the `CREATE LIKE` + `INSERT` approach is slower but safer and idempotent (re-running it on an empty target schema is fine).
2. **Move all tables in one window**, not table-by-table. The dataset is small; coordinated cutover is simpler than a partial-state migration.
3. **Drop old `sky` schema** at the end. Keeping it around as a "safety net" invites someone to accidentally connect a service there and resume the antipattern.
4. **MySQL grants left broad for now.** Per-service users with `GRANT ... ON sky_booking.* TO sky_booking_user@'%'` is the natural follow-up; do not bundle here to avoid scope creep.
5. **Helm initialization** uses a `ConfigMap`-mounted SQL init script for the MySQL chart's `initdbScripts` value. Idempotent (`CREATE DATABASE IF NOT EXISTS ...`).
6. **No app code changes beyond JDBC URL.** Hibernate doesn't care about the schema name; entities don't hardcode schemas (verified during audit).

## Risks / Trade-offs

- **Data move during apply** is the riskiest step. Mitigations:
  - Take a `mysqldump sky` backup before the move.
  - Verify row counts on each table after `INSERT SELECT`.
  - Keep `sky` schema until smoke tests pass; only then `DROP DATABASE sky`.
- **AUTO_INCREMENT discontinuity**: `CREATE TABLE LIKE` does not copy the next-value. After `INSERT SELECT`, `ALTER TABLE sky_<svc>.<tbl> AUTO_INCREMENT = (SELECT MAX(id)+1 FROM sky_<svc>.<tbl>)`. Otherwise next insert reuses old IDs and fails uniqueness checks.
- **Flyway history table movement**: if migrations landed before the split, history rows move too. Otherwise Flyway re-applies V1 on the new schema, which is harmless if migrations are idempotent (use `CREATE TABLE IF NOT EXISTS`).
- **Foreign-key checks**: none cross-schema in our case, but verify with `SHOW CREATE TABLE` per table just in case.
- **Service downtime**: brief (seconds). For a portfolio project, acceptable; document in the cutover plan.
