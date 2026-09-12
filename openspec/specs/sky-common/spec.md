# sky-common Specification

## Purpose
Defines what the shared library may hold and how a consumer may depend on it, so wire types and infrastructure defaults are written once without forcing every module to inherit a runtime dependency it does not need.

## Requirements

### Requirement: Shared library module for cross-service wire types and infra
A `sky-common` Gradle module MUST exist to own (a) cross-service wire types, such as the Kafka payload record, (b) shared web utilities, meaning the header constants, the correlation-id filter, and the REST exception handler, which is supplied as an auto-configured bean rather than as a base class for a service to extend, and (c) the shared infrastructure auto-configurations, covering web, security, OpenAPI, shared property binding and the startup log, each registered in the module's `AutoConfiguration.imports` file, which is the contract: a class not listed there never runs in a consumer whatever annotations it carries. Each MUST be guarded so it contributes nothing to a consumer whose classpath does not support it. A service that needs different handling MUST NOT subclass a shared handler: it replaces one by declaring its own bean of that handler's type, and the shared bean is then not registered, because each is declared `@ConditionalOnMissingBean` on the type it supplies. Adding handling alongside the shared behaviour, rather than replacing it, is a separate advice in the service, which is what all three REST services do today. Every dependency those classes need at runtime MUST be declared `compileOnly` here, so a consumer that does not need Kafka, Spring MVC, springdoc or Spring Data never receives it transitively from this module and instead opts in with its own `implementation` entry. The module MUST NOT contain business or domain logic, which stays per service.

#### Scenario: Kafka payload type is defined once
- **WHEN** sky-booking, sky-offer, and sky-notify need to serialize/deserialize Kafka events
- **THEN** all three depend on `sky-common`'s `KafkaPayloadModel` record, and no duplicate definition exists in any service

#### Scenario: Auto-configuration is opt-in
- **WHEN** a service depends on `sky-common` but brings none of the optional stack, as sky-notify does by applying no web conventions and so pulling neither springdoc nor Spring Data
- **THEN** the springdoc auto-configuration and the Spring Data exception handler both stay unregistered, because each is guarded by a `@ConditionalOnClass` on a type that service does not have, and the context starts without them

#### Scenario: A non-Kafka service never receives Kafka
- **WHEN** a service depends on `sky-common` but declares no Kafka dependency of its own, as sky-message does
- **THEN** no Kafka type reaches its classpath, because `sky-common` declares Spring Kafka `compileOnly`, so the shared Kafka publisher is a class that service cannot reference rather than a bean it has to switch off

#### Scenario: A service replaces a shared handler
- **WHEN** a service declares its own bean of a handler type `sky-common` also supplies
- **THEN** the service's bean is the one in the context, the shared bean is not registered, and no class in the service extends the shared handler

#### Scenario: No domain logic in sky-common
- **WHEN** a contributor attempts to place service business logic into `sky-common`
- **THEN** code review rejects it, and the `compileOnly` split makes most such attempts fail to compile here, because the service-specific dependency the logic would need is not declared. The module's six packages each stay within their stated concern: `config` for bound shared property records, `kafka` for the wire envelope, the shared publisher and the topic names, `openapi` for the documentation auto-configurations and the shared response annotations, `security` for the shared filter chain, permit-lists, JWT wiring and method-security support, `startup` for the startup readiness log, and `web` for the exception handlers, the correlation id, API versioning, and date-time constants. A package outside those concerns is a deliberate addition, not a place to put a service's logic.
