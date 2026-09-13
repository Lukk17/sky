# sky-message

User-to-user messaging service for the Sky platform.

Port: 5553, API prefix `/api/v1`, caller identity from the JWT `email` claim.

---

### What it does

`sky-message` handles direct messages between authenticated users. Users can send messages, retrieve their sent and
received inboxes, and delete messages. Delivery is synchronous REST. Unlike `sky-offer`, `sky-booking` and
`sky-notify`, this service neither produces nor consumes Kafka events.

A send is accepted for any `receiverEmail` that passes validation. Nothing checks the address against an identity
realm, so a message to an address nobody owns is stored and shows up in the sender's sent box. The service contacts
Keycloak for one thing only: the signing keys it validates bearer tokens against.

---

### Endpoints

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/messages/sent` | List messages sent by the authenticated user (paged) | |
| `GET` | `/api/v1/messages/received` | List messages received by the authenticated user (paged) | |
| `POST` | `/api/v1/messages` | Send a new message | `MessageDTO` |
| `DELETE` | `/api/v1/messages/{messageId}` | Delete a message | |

Pagination. `GET /api/v1/messages/sent` and `GET /api/v1/messages/received` both return a Spring Data `Page<MessageDTO>` envelope. Use the `page` and `size` query parameters to control pagination (default: page 0, size 20):

```text
GET /api/v1/messages/received?page=0&size=10
```

The response body has the following structure:

```json
{
  "content": [ { ...MessageDTO... } ],
  "totalElements": 8,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

`MessageDTO`:

```json
{
  "text": "Hello, is this offer still available?",
  "receiverEmail": "someone@example.com"
}
```

The sender's identity comes from the `email` claim of the validated bearer token, read through
`SecurityUtils.currentUserEmail()` in `sky-common`. The service does not read the `x-auth-request-email` header that
`oauth2-proxy` forwards.

Swagger UI: `http://localhost:5553/swagger-ui/index.html` (local) or
`https://skycloud.luksarna.com/msg/swagger-ui/index.html` (cluster).

---

### Architecture

Uses hexagonal (ports-and-adapters):

- `domain/model`, `domain/exception`: the core model and its exceptions.
- `domain/ports/inbound`: `MessageService`, the driving port the controller calls.
- `domain/ports/outbound`: `MessageRepository`, the driven port the JPA adapter implements.
- `domain/service`: `MessageServicePrimary`, the domain implementation.
- `adapters/inbound/api`: `MessageController`. Reads the sender from the validated JWT through `SecurityUtils.currentUserEmail()`, never from a request header.
- `adapters/dto`: wire DTOs.
- `config`, `config/propertyBind`: Spring wiring and bound properties.

Plain Spring MVC stack (`spring-boot-starter-web`). There is no Kafka dependency in this service. Do not add it
without a deliberate design decision.

Schema versioning via Flyway. Migrations in [src/main/resources/db/migration/](src/main/resources/db/migration/), against the shared `sky` database.

---

### Key environment variables

| Variable | Default | Notes |
|---|---|---|
| `MESSAGE_PORT` | `5553` | Service port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://host.docker.internal:5432/sky` | JDBC URL |
| `POSTGRES_USER` | none, required | Database username. Unset, the service refuses to start and names the variable |
| `POSTGRES_PASSWORD` | none, required | Database password. Unset, the service refuses to start and names the variable |
| `OAUTH2_ISSUER_URI` | `https://keycloak.test:9443/realms/sky` | OIDC issuer used to validate bearer tokens |
| `OAUTH2_AUDIENCE` | unset | Set to `sky-backend` to enforce the audience claim |

`OAUTH2_ISSUER_URI` is the only Keycloak setting this service has. There is no client id and no client secret: the
service never acts as an OAuth2 client, it only validates the tokens it is handed.

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-message:test
```

Integration and repository tests use a Testcontainers `postgres:17-alpine` container, wired through `@ServiceConnection`. There is no Kafka container, this service has no broker. Docker must be running: there is no in-memory fallback, because H2 is gone from this module and from the version catalogue.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Platform overview, modules, build, ports |
| [AGENTS.md](AGENTS.md) | Module-local agent and coding conventions |
| [src/main/resources/db/migration/](src/main/resources/db/migration/) | Flyway SQL migrations owned by this service |
| [../config/local-dev/local_README.md](../config/local-dev/local_README.md) | Running the platform locally |
| [../docs/api/README.md](../docs/api/README.md) | Bruno collection and OpenAPI specs |
