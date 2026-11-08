# AGENTS.md: sky-contract

Module-local guidance for `sky-contract`. Read the root [AGENTS.md](../../../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-contract` is a build-time library, not a deployable service. It has no `main` class, no `bootJar`, no port and no
runtime configuration. It runs OpenAPI Generator once over the published contract and exposes the generated API
interfaces and DTOs to the three REST services.

The published contract is [docs/api/contract/openapi.yaml](../../../docs/api/contract/openapi.yaml). It is the only
published API document, and this module is the only OpenAPI Generator in the backend. No service runs
`openApiGenerate`, and no service carries its own generator configuration, templates or import mappings.

Dependants:

- `sky-offer` implements `com.lukk.sky.contract.api.OffersApi` and `OwnerOffersApi`.
- `sky-booking` implements `com.lukk.sky.contract.api.BookingsApi`.
- `sky-message` implements `com.lukk.sky.contract.api.MessagesApi`.
- `sky-notify` has no REST contract and does not depend on this module. It keeps its own `adapters/dto`.

## Generated Packages

The generator writes into `build/generated/contract/src/main/java` under two packages:

- `com.lukk.sky.contract.api`: the Spring MVC interfaces, one per contract tag. They carry the request mappings,
  parameters, response types and springdoc annotations.
- `com.lukk.sky.contract.dto`: the request and response models. The collection responses are the typed pages
  `OfferPage`, `BookingPage` and `MessagePage`, and the error body is `ProblemDetail`.

The generated Java is build-time only. It is never committed and lives under `build/`, so do not add it to Git and do
not edit it by hand. The module contains no hand-written production code. The only source file under `src/` is the
generation guard test.

## Build Inputs

- Contract: `docs/api/contract/openapi.yaml`, resolved from the repository root in
  [build.gradle.kts](build.gradle.kts). It is declared as an input of `openApiGenerate`, so a contract edit invalidates
  the generated sources.
- Generator: `org.openapi.generator`, with the version pinned in
  [gradle/libs.versions.toml](../gradle/libs.versions.toml) as `openapi-generator`.
- Plugins: `sky.java-library-conventions` and `sky.web-conventions`. The web conventions supply Spring Web and
  validation, which the generated interfaces and DTOs use. The library conventions make this a plain Java library.
- Import mappings: `ProblemDetail` maps to `org.springframework.http.ProblemDetail`, and `Pageable` maps to
  `org.springframework.data.domain.Pageable`. Keep both in the generator configuration rather than declaring copies of
  those types in the contract.
- Generator options live in [build.gradle.kts](build.gradle.kts): interface-only output, Spring Boot 4, tags as
  interfaces, no default interface methods, springdoc annotations, Jakarta EE, built-in validation, no
  `JsonNullable`, and `additionalModelTypeAnnotations` adding `JsonInclude(NON_NULL)`.

There are no Mustache templates in this module. Customisation belongs in the generator options, or in the contract
itself. Adding a template creates a second place that decides what a generated type looks like.

## Generation Guard

[ContractGenerationTest](src/test/java/com/lukk/sky/contract/ContractGenerationTest.java) is the guard. It reflects over
the generated classes and fails when the contract stops producing the properties the services rely on:

- Validation annotations on the generated DTOs, including `@Pattern` and `@Size` on `OfferDTO.hotelName`, `@Email` on
  `MessageDTO.receiverEmail`, and no `@NotNull` on the nullable `OfferEditDTO.hotelName`.
- `JsonInclude(NON_NULL)` on the generated models.
- `LocalDate` for `BookingDTO.bookedDate`, with no string constraints added to it.
- `List<OfferDTO>`, `List<BookingDTO>` and `List<MessageDTO>` as the `content` type of the three typed pages.
- `OffersApi.search` returning `OfferPage` and binding `term` as a `@RequestParam`.
- `OwnerOffersApi` exposing every owner operation.

Run it after any contract change:

```bash
./apps/backend/gradlew -p apps/backend :sky-contract:test
```

The module does not opt out of the JaCoCo gate. Do not disable `jacocoTestCoverageVerification` here.

## Docker

The image builds use the repository root as the Docker context. The `sky-offer`, `sky-booking` and `sky-message`
Dockerfiles copy `apps/backend/sky-contract` and `docs/api/contract` into the build stage, because those three services
compile against this module and need the contract present during the build. `sky-notify` and `sky-gateway` copy
neither, because neither depends on the contract.

When a Docker build fails in `sky-contract`, check that both copies are still present in the Dockerfile and that the
build was started from the repository root, not from `apps/backend`.
