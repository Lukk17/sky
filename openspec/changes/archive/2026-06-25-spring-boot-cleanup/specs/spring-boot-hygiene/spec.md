## ADDED Requirements

### Requirement: Only the starters actually used are on the classpath
No service may declare a Spring Boot starter it does not use. Specifically: `spring-boot-starter-data-rest` MUST NOT be declared by any service; services that use a servlet stack MUST NOT declare `spring-boot-starter-webflux`; services MUST declare exactly one springdoc-openapi UI starter matching their stack.

#### Scenario: Auditing dependencies
- **WHEN** a contributor runs `./gradlew :sky-booking:dependencies` (and the other services)
- **THEN** `spring-boot-starter-data-rest` does not appear; only `spring-boot-starter-web` appears (not `webflux`) in REST services; exactly one springdoc starter is present per REST service

### Requirement: No default credentials in committed config
No `application.yaml` or related profile config MUST carry a default value for a credential, secret, or password env-var reference. Missing env vars MUST cause startup to fail loudly.

#### Scenario: Starting a service without required env vars
- **WHEN** a developer starts sky-booking without setting `MYSQL_PASS`
- **THEN** the service fails to start with a clear "property required" error; it does not silently use a hardcoded default

### Requirement: CORS defaults are restrictive
The default CORS allowed-origins value in `application.yaml` MUST be a named known origin (the production frontend), not a wildcard. Dev profile overlays may relax to localhost origins; wildcards are forbidden in defaults.

#### Scenario: Default-profile CORS
- **WHEN** the application boots without an `ACCESS_CONTROL_ALLOW_ORIGIN` env var
- **THEN** the configured CORS origin is the production frontend hostname, not `*`
