## ADDED Requirements

### Requirement: Each service owns its own MySQL schema
sky-booking, sky-offer, and sky-message MUST each connect to a dedicated MySQL schema (`sky_booking`, `sky_offer`, `sky_message`). No service may read or write tables outside its own schema. Cross-service data access MUST go through the service's REST API or Kafka events.

#### Scenario: Verifying schema isolation
- **WHEN** an operator runs `SHOW DATABASES` against the MySQL instance after migration
- **THEN** `sky_booking`, `sky_offer`, `sky_message` exist; the legacy `sky` schema does not

#### Scenario: A service queries only its own schema
- **WHEN** sky-booking needs offer information
- **THEN** it calls sky-offer over HTTP (`GET /api/internal/owner/offers/{id}`); it does not connect to `sky_offer` or any schema other than `sky_booking`

### Requirement: Per-service Flyway history lives in the service's own schema
The `flyway_schema_history` table MUST live in the schema owned by the service whose migrations it tracks. Migration ordering is per-service; no cross-schema coordination is required.

#### Scenario: Inspecting migration state
- **WHEN** an operator runs `SELECT version FROM sky_booking.flyway_schema_history`
- **THEN** the result lists only booking-service migrations; offer-service migrations live in `sky_offer.flyway_schema_history`
