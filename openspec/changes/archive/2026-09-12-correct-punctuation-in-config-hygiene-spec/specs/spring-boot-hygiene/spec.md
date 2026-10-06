## MODIFIED Requirements

### Requirement: Only the starters actually used are on the classpath
No module may declare a Spring Boot starter it does not use. Three rules make that checkable. `spring-boot-starter-data-rest` MUST NOT be declared anywhere. A module on the servlet stack MUST NOT declare `spring-boot-starter-webflux`, which leaves sky-gateway as the one reactive module, and it is reactive through `spring-cloud-starter-gateway-server-webflux` rather than through the Spring Boot starter, because Spring Cloud Gateway requires Netty and breaks when Tomcat reaches the classpath. Exactly one springdoc-openapi UI starter MUST be on the classpath of a module that serves a REST API and none MUST be on the classpath of a module that does not, which is achieved by declaring it once in the shared web convention plugin rather than per module, so the three REST services get a Swagger UI by applying that plugin and sky-notify and sky-gateway get none by not applying it.

#### Scenario: Auditing dependencies
- **WHEN** a contributor runs `./gradlew :sky-booking:dependencies`, and the same for the other modules
- **THEN** `spring-boot-starter-data-rest` appears nowhere, `spring-boot-starter-webflux` appears in no servlet-stack module, and exactly one springdoc starter appears for each of the three REST services and none for the other modules

### Requirement: No default credentials in committed config
No `application.yaml`, and no profile-specific configuration file beside it, may carry a default value for a credential, a secret or a password. The environment variable reference MUST stand alone, as `${POSTGRES_PASSWORD}` does, so a missing value fails startup rather than falling back to something a deployment did not choose. Missing environment variables MUST cause startup to fail loudly.

#### Scenario: Starting a service without required env vars
- **WHEN** a developer starts sky-booking without setting `POSTGRES_PASSWORD`
- **THEN** the service fails to start with a clear message naming the unresolved property, and it does not silently fall back to a hardcoded default

### Requirement: CORS defaults are restrictive
The default CORS allowed-origins value in `application.yaml` MUST be a named known origin, which is the production frontend, and MUST NOT be a wildcard. A profile overlay, which in this repository means the `local` profile, MAY relax the list to localhost origins, and a wildcard remains forbidden in the default whatever a profile does.

#### Scenario: Default-profile CORS
- **WHEN** the application boots without an `ACCESS_CONTROL_ALLOW_ORIGIN` environment variable
- **THEN** the configured CORS origin is the production frontend hostname rather than `*`
