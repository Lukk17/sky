## MODIFIED Requirements

### Requirement: Authenticated and unauthenticated paths are both tested
Every controller test class MUST include at least one test where the request is authenticated and one where it is not. The authentication mechanism in tests MUST match the production mechanism, which is the JWT resource-server chain every service installs, so a test authenticates by presenting a JWT rather than by setting an identity header. A test asserting a rejection MUST assert the status the service's exception handler actually maps that failure to, not a placeholder pair.

#### Scenario: Unauthenticated request to a protected endpoint
- **WHEN** a test sends a request to `POST /api/v1/bookings` with no bearer token
- **THEN** the controller returns 401 and the test asserts that status

#### Scenario: A rejection is asserted as the status its handler returns
- **WHEN** a controller test in `sky-booking` or `sky-offer` asserts the status of a rejected request
- **THEN** the asserted status is the one that service's exception handler maps the failure to, drawn from this set:
  - 400 for a domain exception, a bean-validation failure, an unknown sort property, or a value the database refused
  - 401 for a missing or invalid bearer token
  - 403 for an access-denied failure
  - 404 for a booking or an offer that does not exist
  - 409 for an event-sequence conflict in either service, and for an already-booked date in `sky-booking`
  - 502 for a dependency that answered unusably
  - 503 carrying a `Retry-After` header for a dependency outage
