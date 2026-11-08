# API contract

One OpenAPI 3.1 document, [openapi.yaml](openapi.yaml), is the published API document for the sky platform. It is the only document a client should read, and it is the only input to code generation.

This page explains what the contract owns, what is generated from it, and how to change it without breaking the backend or the frontend.

---

### What the contract owns

The contract is the single source of the HTTP surface. It defines every path, method, request body, response body, status code and validation rule that a caller can rely on.

The backend DTOs and API interfaces come from this file. The Angular client comes from this file. No second description of the API exists.

Each service still serves a springdoc document at runtime, at `/v3/api-docs`, and springdoc builds Swagger UI from the controller annotations at `/swagger-ui/index.html`. That document is a runtime view for exploring a running service. It is not committed, it is not published, and it is never an input to code generation.

The contract is not bundled. [redocly.yaml](redocly.yaml) configures a lint run over the source document, and CI fails when the lint fails. There is no generated bundle file and no committed per-service document.

---

### Generated outputs

Backend generation runs once, in the `sky-contract` module. The module applies `org.openapi.generator` and reads [openapi.yaml](openapi.yaml). It produces two packages:

- `com.lukk.sky.contract.api`: the Spring MVC interfaces, one per tag, with the request mappings, parameters and response types.
- `com.lukk.sky.contract.dto`: the request and response models, including `OfferDTO`, `BookingDTO`, `MessageDTO`, `OfferPage`, `BookingPage`, `MessagePage` and `ProblemDetail`.

The generated Java is build-time only. It lives under `apps/backend/sky-contract/build/generated/contract` and is never committed. `sky-offer`, `sky-booking` and `sky-message` depend on `:sky-contract` and implement the generated interfaces. `sky-notify` has no REST contract and keeps its own wire types.

The TypeScript client is different: it is committed at [apps/frontend/sky-view/src/app/api-generated](../../../apps/frontend/sky-view/src/app/api-generated) and freshness-gated. [check-client-freshness.sh](../../../config/scripts/check-client-freshness.sh) regenerates it into a temporary directory and diffs the result against the committed copy. The package name inside the generated README is `sky-api-client`.

`sky-contract` is the only module that runs OpenAPI Generator. No service runs `openApiGenerate`, and no service carries its own generator configuration, templates or import mappings.

---

### Types, validation and search

Pagination is typed. Collection responses use `OfferPage`, `BookingPage` and `MessagePage`, each with a `content` list of the matching DTO. There is no generic `Page` type in the contract and no generic page wrapper in the generated client.

Validation lives in the contract. Required fields, minimum and maximum lengths, patterns and email formats become Jakarta Bean Validation annotations on the generated classes. A rule is changed in [openapi.yaml](openapi.yaml) and nowhere else. Do not add a second validation rule in a controller or a mapper.

Search is `GET /api/v1/search?term=...`. The `term` query parameter is required, must not be blank, and must be between 1 and 100 characters. A request outside those bounds is answered with 400.

The contract imports two existing framework types instead of generating copies: `ProblemDetail` maps to `org.springframework.http.ProblemDetail`, and `Pageable` maps to `org.springframework.data.domain.Pageable`.

---

### Change a schema safely

1. Edit [openapi.yaml](openapi.yaml). Change the schema, parameter or response that owns the rule.
2. Lint the document with Redocly.
3. Regenerate the backend interfaces and DTOs, then compile the three services.
4. Run the generation guard test. It checks the properties the contract is expected to produce, such as validation annotations, typed page content and the search parameter binding.
5. Regenerate the Angular client with the freshness script, which writes the client into a temporary directory and diffs it against the committed copy.
6. Build and test the frontend.

The commands are below. Run all of them from the repository root.

Lint the contract:

```bash
npx --yes @redocly/cli@2.73.0 lint docs/api/contract/openapi.yaml --config docs/api/contract/redocly.yaml
```

Regenerate the backend code:

```bash
./apps/backend/gradlew -p apps/backend :sky-contract:openApiGenerate
```

Run the backend generation guard:

```bash
./apps/backend/gradlew -p apps/backend :sky-contract:test --tests com.lukk.sky.contract.ContractGenerationTest
```

Regenerate and check the Angular client:

```bash
bash config/scripts/check-client-freshness.sh
```

A change that adds or renames a schema is not finished until the three services compile against the new generated interfaces and the committed client in [apps/frontend/sky-view/src/app/api-generated](../../../apps/frontend/sky-view/src/app/api-generated) matches the contract.

---

### What the contract does not own

Authentication and session handling stay outside the contract. Login, the gateway session cookie, CSRF tokens and redirects are browser and edge behaviour, not API schema. The contract describes the bearer token requirement on operations, but it does not describe how a session is created or stored.

Kafka event payloads and the STOMP WebSocket payload are also outside the contract. They are internal or real-time transports, not the REST surface.

The runtime error behaviour is code, not contract. The contract declares that an error response has the `ProblemDetail` shape and which statuses an operation can return. The handler that decides between 400, 404, 502 and 503 lives in the services and in `sky-common`.

---

### Related documents

- [docs/api/README.md](../README.md): the Bruno collection, token minting and gateway routing.
- [apps/backend/sky-contract/AGENTS.md](../../../apps/backend/sky-contract/AGENTS.md): the generated module, its build inputs and the generation guard.
- [apps/frontend/sky-view/README.md](../../../apps/frontend/sky-view/README.md): the consuming Angular application.
