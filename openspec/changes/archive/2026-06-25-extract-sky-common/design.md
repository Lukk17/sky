## Context

Microservices repos hit the "shared module" question early. The default answer in many shops is "never share code between services" — but that rule was forged in polyglot orgs where Service A might be Java and Service B Go. In a single-language monorepo, *some* sharing is correct: wire contracts and cross-cutting infra (exception handler shape, header constants, Kafka serdes). Drift in `KafkaPayloadModel` (the `message` vs `payload` divergence already observed) proves the cost of not sharing.

The fence to defend: `sky-common` may contain **wire types and infra**, never **business logic**. A `BookingService` that imports `sky-common.OfferLookup` would re-introduce coupling we just escaped.

## Goals / Non-Goals

**Goals:**
- Single definition of every Kafka payload type and shared header constant.
- Single base for the global exception handler shape so error responses look identical across services.
- Spring Boot auto-configuration mechanism — services that don't use Kafka don't pull in Kafka producer beans by accident.
- Zero domain logic in `sky-common`.

**Non-Goals:**
- Shared DTOs for REST APIs (each service owns its own request/response DTOs).
- Shared entities (`@Entity` stays per-service).
- Shared service interfaces or port abstractions (each service defines its own ports per hexagonal).
- A "utils" dumping ground.

## Decisions

1. **Module is not a Spring Boot application.** No `@SpringBootApplication`, no `bootJar`. Just a plain Java library JAR with `@Configuration` classes exposed via auto-config imports.
2. **Auto-config over imports.** Each Kafka-using service gets the producer beans through Spring Boot auto-config discovery (`AutoConfiguration.imports`), not via `@Import`. This makes `sky-common` purely additive — including it as a dep doesn't break a service that doesn't want Kafka.
3. **Field rename `message` → `payload`** at the same commit. Backed by `@JsonAlias({"message", "payload"})` on the record component to keep wire-compat with any in-flight messages during rolling deploy. Removable in a follow-up after a deploy cycle.
4. **Exception handler is an `abstract class`, not an interface or `@ControllerAdvice`.** Spring's `@RestControllerAdvice` discovery + `@ExceptionHandler` inheritance work cleanly only when the concrete advice extends a non-`@ControllerAdvice` abstract class.
5. **No Lombok in `sky-common`?** Decision: yes, allow it (records cover most cases, but the abstract handler benefits from `@Slf4j`).
6. **Versioning**: `sky-common` is versioned with the repo, not independently. Internal contract.

## Risks / Trade-offs

- **The slippery slope**: every shared-module repo eventually grows a "utils" package full of unrelated helpers. Mitigation: ArchUnit rule in `hexagonal-enforcement-archunit` to keep `sky-common.*` packages limited to the agreed namespaces (`kafka`, `web`, `time`).
- **Bidirectional compile-time coupling avoided**: `sky-common` must never depend on a service module. Enforced by the multi-project DAG.
- **Spring Boot version coupling**: `sky-common` compiles against whatever Spring/Kafka version the catalog pins. Bumping the catalog bumps `sky-common`, which is fine and intentional.
- **Field rename**: the `@JsonAlias` keeps it safe for one deploy cycle; remove afterward to avoid the type tolerating two field names forever.
