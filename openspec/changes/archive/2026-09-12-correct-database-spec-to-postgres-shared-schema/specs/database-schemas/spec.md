## RENAMED Requirements

- FROM: `### Requirement: Per-service Flyway history lives in the service's own schema`
- TO: `### Requirement: Per-service Flyway history lives in its own table in the shared schema`

## REMOVED Requirements

### Requirement: Each service owns its own MySQL schema
**Reason**: The decision this requirement recorded was reversed. The project moved from MySQL to PostgreSQL and did not carry the per-service schema split across: all three stateful services point at one database named `sky` and every table lands in `public`. The schemas `sky_booking`, `sky_offer` and `sky_message` exist in no live source file, chart or compose file, and the `sky` schema this requirement asserted was gone is the only one there is. The archived change `2026-06-25-db-per-service-schemas` remains the record of the layout that was abandoned.
**Migration**: The ownership rule it carried is not dropped, it is restated against the real layout by the requirement "Each service owns its own tables in the shared sky database", which keeps the prohibition on reading or writing another service's tables and keeps cross-service access on REST or Kafka.

## MODIFIED Requirements

### Requirement: Per-service Flyway history lives in its own table in the shared schema
Each service's Flyway history MUST live in a history table named for that service, so three services sharing one schema cannot compete for one history table. Each service MUST set `spring.flyway.table` to its own name: `flyway_schema_history_booking` for sky-booking, `flyway_schema_history_offer` for sky-offer, `flyway_schema_history_message` for sky-message. Migration ordering is per-service, and no cross-service coordination is required, because a version number is only ever compared against the history table of the service that owns it.

#### Scenario: Inspecting migration state
- **WHEN** an operator runs `SELECT version FROM flyway_schema_history_booking` against the `sky` database
- **THEN** the result lists only booking-service migrations, and offer-service migrations are listed by `flyway_schema_history_offer` in that same database

## ADDED Requirements

### Requirement: Each service owns its own tables in the shared sky database
sky-booking, sky-offer, and sky-message MUST each connect to the one PostgreSQL database named `sky`, and their tables all live in its `public` schema. Ownership is by table rather than by schema: a service MUST read and write only the tables its own migrations create, and MUST NOT read or write another service's tables even though they are reachable on the same connection. Cross-service data access MUST go through the owning service's REST API or Kafka events.

#### Scenario: Verifying the deployed database layout
- **WHEN** an operator lists the databases on the PostgreSQL instance and then lists the tables in `public`
- **THEN** one application database named `sky` is present, no `sky_booking`, `sky_offer` or `sky_message` database or schema exists, and `public` holds the tables of all three services alongside their three Flyway history tables

#### Scenario: A service queries only the tables it owns
- **WHEN** sky-booking needs offer information
- **THEN** it calls sky-offer over HTTP, and it issues no query against `offer` or `offer_event` even though both are in the schema its own connection is already attached to
