# sky-common Specification

## Purpose
TBD - created by archiving change extract-sky-common. Update Purpose after archive.
## Requirements
### Requirement: Shared library module for cross-service wire types and infra
A `sky-common` Gradle module MUST exist to own (a) cross-service wire types (Kafka payload records), (b) shared web utilities (header constants, exception handler base class), and (c) conditional auto-configurations for shared infrastructure (Kafka producer beans). The module MUST NOT contain business or domain logic; that stays per service.

#### Scenario: Kafka payload type is defined once
- **WHEN** sky-booking, sky-offer, and sky-notify need to serialize/deserialize Kafka events
- **THEN** all three depend on `sky-common`'s `KafkaPayloadModel` record; no duplicate definitions exist in any service

#### Scenario: Auto-configuration is opt-in
- **WHEN** a service depends on `sky-common` but does not use Kafka (e.g., sky-message)
- **THEN** the Kafka producer beans are not registered, because their auto-configuration is `@ConditionalOnClass(KafkaTemplate.class)`

#### Scenario: No domain logic in sky-common
- **WHEN** a contributor attempts to place service business logic into `sky-common`
- **THEN** code review (and the ArchUnit rule in `hexagonal-enforcement`) rejects it; `sky-common` packages are limited to `kafka`, `web`, and `time` namespaces

