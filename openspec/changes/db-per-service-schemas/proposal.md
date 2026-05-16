## Why

Today sky-booking, sky-offer, and sky-message all connect to the same MySQL schema `sky`. The audit confirmed no cross-service JPA relationships exist (no `@ManyToOne` linking entities across services — booking stores `offerId` as a plain `String` and queries sky-offer over REST). So the *coupling* is shallow; what remains is just shared namespace and the wrong cultural signal.

Splitting into per-service schemas (`sky_booking`, `sky_offer`, `sky_message`) on the same MySQL instance:
- Removes the shared-namespace antipattern visibly from JDBC URLs.
- Lets each Flyway history table live in its own schema (cleaner ops).
- Lets a single service's schema be dumped/restored independently.
- Is reversible and low-risk because no joins span services.

## What Changes

- **Create** three new schemas: `sky_booking`, `sky_offer`, `sky_message`.
- **Migrate data** from the old `sky` schema: per-service, move tables that belong to that service into its new schema. Each service owns: booking → `booking`, `booking_event`; offer → `offer`, `offer_event`; message → `message`. (Other tables that exist accidentally in `sky` — investigate during apply.)
- **Update** each service's JDBC URL in `application.yml` to point at its own schema.
- **Update** `config/k8s/helm/db/mysql/` chart values to create all three schemas on initial provision (init SQL or post-install hook).
- **Update** each service's V1 Flyway migration (if migrations have already landed via `db-migrations-flyway`) to target the new schema. If they have not, no change there.
- **Drop** the old `sky` schema once migration is verified.

## Capabilities

### New Capabilities
- _None._ This is a runtime reconfiguration; capabilities are unchanged.

### Modified Capabilities
- `db-migrations` (from `db-migrations-flyway`): each service's Flyway now targets its own schema; baseline ceremony adjusted.

## Impact

- **JDBC URLs change**:
  - sky-booking: `jdbc:mysql://.../sky` → `jdbc:mysql://.../sky_booking`
  - sky-offer: `jdbc:mysql://.../sky` → `jdbc:mysql://.../sky_offer`
  - sky-message: `jdbc:mysql://.../sky` → `jdbc:mysql://.../sky_message`
- **Local dev**: each developer must create the three new schemas. Helm + a Bash/SQL bootstrap script handles cluster.
- **Production**: requires a one-time migration window (or a careful zero-downtime split — see design). Acceptable since this is a hobby/portfolio cluster, not a 99.999% SaaS.
- **Backups**: per-schema dumps replace the monolithic one. Cleaner.
- **CI tests**: H2 doesn't really care; Testcontainers (after `test-modernization`) will create three databases — minor overhead, no risk.
- **Risk**: medium. Data movement is the real risk; mitigated by SQL-level move + verification.
- **Dependency order**: depends on `db-migrations-flyway`. Should land before `kafka-reliability` only if DLQ tables (if any) live in the DB; otherwise independent.
