# architecture Specification

## Purpose
TBD - created by archiving change hexagonal-enforcement-archunit. Update Purpose after archive.
## Requirements
### Requirement: Hexagonal layer separation is enforced by ArchUnit
Every service MUST run ArchUnit tests that fail the build when the hexagonal dependency direction is violated. Specifically: `adapters` may depend on `domain` and `sky.common`; `domain` may depend only on itself, `sky.common`, and `java.*`; `config` may depend on both `adapters` and `domain`. No cross-service imports are permitted.

#### Scenario: A controller bypasses the service layer
- **WHEN** a developer adds a field injecting a `*Repository` directly into a controller (skipping the service/use-case layer)
- **THEN** the service's ArchUnit test fails the build with a layer-violation message

#### Scenario: A domain class imports a Spring web type
- **WHEN** a developer adds `import org.springframework.web.client.RestClient` to a class in `domain.*`
- **THEN** the ArchUnit cross-cutting rule fails the build

### Requirement: Canonical package layout per service
Every service MUST organize source under: `adapters.{api,inbound,outbound,persistence}`, `domain.{model,ports,service,exception}`, `config`. Class-naming conventions (Port suffix for ports, Controller suffix for REST controllers, ServicePrimary for use-case implementations) MUST hold across all services.

#### Scenario: New contributor navigating across services
- **WHEN** a contributor finds the booking controller at `sky-booking/.../adapters/api/BookingController.java`
- **THEN** the same relative path under another service (`sky-offer/.../adapters/api/OfferApiController.java`) holds the equivalent controller

