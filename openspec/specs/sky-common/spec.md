# sky-common Specification

## Purpose
Defines what the shared library may hold and how a consumer may depend on it, so wire types and infrastructure defaults are written once without forcing every module to inherit a runtime dependency it does not need.

## Requirements

### Requirement: Shared library module for cross-service wire types and infra
A `sky-common` Gradle module MUST exist to own (a) cross-service wire types, such as the Kafka payload record, (b) shared web utilities, meaning the header constants, the correlation-id filter, and the shared error handling, which is supplied as auto-configured beans rather than as a base class for a service to extend, and which is not all of it advice, because the requirement `One error shape, held by a last resort that is not an advice` fixes where each part of it sits, and (c) the shared infrastructure auto-configurations, covering web, security, OpenAPI, shared property binding and the startup log, each registered in the module's `AutoConfiguration.imports` file, which is the contract for an auto-configuration class: one not listed there never runs in a consumer whatever annotations it carries. Each MUST be guarded so it contributes nothing to a consumer whose classpath does not support it. That registration contract binds the auto-configuration class alone. A bean declared on a class already listed reaches every consumer through that entry and MUST NOT be given an entry of its own, and a shared type Spring Boot discovers through a different mechanism MUST be registered in that mechanism's own file, which today is `META-INF/spring.factories` holding the one `FailureAnalyzer` this module ships and nothing besides. A service that needs different handling MUST NOT subclass a shared handler: it replaces one by declaring its own bean of the type that handler is keyed on, and the shared bean is then not registered, because each is declared `@ConditionalOnMissingBean`, keyed on its own type where it has no framework supertype and on that supertype where it has one, as the shared advice extending Spring's `ResponseEntityExceptionHandler` does. Adding handling alongside the shared behaviour, rather than replacing it, is a separate advice in the service, which is what all three REST services do today, and such an advice MUST NOT extend a keyed supertype, because that replaces the shared bean instead of adding to it. Every dependency those classes need at runtime MUST be declared `compileOnly` here, so a consumer that does not need Kafka, Spring MVC, springdoc or Spring Data never receives it transitively from this module and instead opts in with its own `implementation` entry. The module MUST NOT contain business or domain logic, which stays per service.

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
- **WHEN** a service declares its own bean of the type a shared handler is keyed on, which is that handler's own type, or the framework supertype it extends where it has one
- **THEN** the service's bean is the one in the context, the shared bean is not registered, and no class in the service extends the shared handler

#### Scenario: No domain logic in sky-common
- **WHEN** a contributor attempts to place service business logic into `sky-common`
- **THEN** code review rejects it, and the `compileOnly` split makes most such attempts fail to compile here, because the service-specific dependency the logic would need is not declared. The module's six packages each stay within their stated concern: `config` for bound shared property records, `kafka` for the wire envelope, the shared publisher and the topic names, `openapi` for the documentation auto-configurations and the shared response annotations, `security` for the shared filter chain, permit-lists, JWT wiring and method-security support, `startup` for the startup readiness log, and `web` for the shared error responses, meaning the advices and the last-resort handler that is deliberately not one, the correlation id, API versioning, and date-time constants. A package outside those concerns is a deliberate addition, not a place to put a service's logic.

### Requirement: One error shape, held by a last resort that is not an advice
Every error a service in this repository answers with a body MUST be an RFC 9457 problem detail served as `application/problem+json`, and that MUST hold for a failure nobody anticipated exactly as it holds for one a service mapped deliberately. One parser MUST cover the whole error surface of an API, so no second body shape may be reachable through a request the service dispatches to one of its own endpoints. An empty body is not a second shape: the 401 and 403 the security filter chain answers carry a status and a `WWW-Authenticate` header and nothing else, which is declared once by the shared response annotation every secured endpoint carries and which the published API documents therefore report rather than assert, because those documents are generated from the annotations on the controllers instead of being maintained by hand. A 403 a service raises from a rule of its own is a different answer on the same status, and it carries a problem detail like every other failure a service maps deliberately.

The guarantee is bounded, and the bound is the dispatcher. A failure raised in the servlet filter chain never reaches it and still lands on the container error page with that page's flat body. Nothing in this repository throws there today, and the bound is written down so nobody reads the guarantee as wider than it is. It is not licence for a second shape inside the covered surface.

The handler of last resort MUST NOT be a controller advice. It MUST be registered outside the advice set and sorted behind the whole of it, so every mapping a service declares deliberately answers first and the catch-all sees only what every other handler declined. An ordering annotation MUST NOT be accepted as a substitute, because the ordering it would rest on does not exist: advice resolution returns from the first advice whose handler matches the exception at all, and an advice that declares no order already sits at the lowest precedence value there is, so nothing can be made to sort behind the three services' own advices and a tie between them falls back to bean discovery order. A catch-all inside the advice set would therefore be free to take a 503, a 502, a 409 or a 404 away from the handler that meant it, and it would do so silently. Registering it as a `HandlerExceptionResolver` behind the composite that holds the entire advice set gives the ordering structurally, which is why its position, and not only its behaviour, is what this requirement fixes.

The last-resort answer MUST be 500 and MUST disclose nothing about the cause: no exception type, no exception message, no class name and no package name, because an unanticipated failure is the case whose message is likeliest to carry something internal. It MUST carry no `Retry-After`, because nothing at that point knows that waiting would help. It MUST log the failure at error with the full stack, because resolving an exception stops the container logging it and this becomes the only place the failure is visible, and the response MUST carry the correlation id that log line carries.

#### Scenario: A failure nobody mapped
- **WHEN** a request to an endpoint of a service fails with an exception neither that service nor `sky-common` declares a handler for, such as a null dereference inside an offer edit
- **THEN** the caller receives 500 as `application/problem+json`, with `title` `Internal Server Error`, an `instance` naming the request path and a fixed `detail` that names neither the exception type nor its message, and the service logs the failure at error with the full stack under the correlation id the response header carries

#### Scenario: A deliberate mapping still wins
- **WHEN** a booking request fails because sky-offer is unreachable, which sky-booking maps to 503 with `Retry-After: 10`
- **THEN** the caller receives that status, that header and that handler's own `detail`, the last-resort 500 answers nothing, and the same holds for every other status the three REST services map, 502, 409, 404 and 400 included

#### Scenario: A catch-all written as an advice
- **WHEN** a contributor proposes the handler of last resort as a `@ControllerAdvice` or `@RestControllerAdvice`, at any order value
- **THEN** the design is rejected, because no order value sorts behind an advice that declares none, so the catch-all could no longer be guaranteed to run last and the first deliberate mapping it shadowed would surface as a wrong status in production rather than as a failing build

#### Scenario: A consumer that is not a REST service
- **WHEN** a module depends on `sky-common` without exposing a REST surface, as sky-notify does with a servlet container it runs only for a WebSocket handshake, and as sky-gateway does on a reactive stack carrying no servlet API at all
- **THEN** the shared error handling contributes nothing: sky-notify holds the beans and routes no request of its own through them, sky-gateway never loads the auto-configuration and cannot link the types, and both contexts start
