# sky-message

*User-to-user messaging service for the Sky platform.*

Port: **5553** | API prefix: `/api/v1` | Identity header: `x-auth-request-email`

---

### What it does

`sky-message` handles direct messages between authenticated users. Users can send messages, retrieve their sent and
received inboxes, and delete messages. Delivery is synchronous REST; unlike the other services, `sky-message` does
not produce or consume Kafka events.

---

### Endpoints

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/v1/messages/sent` | List messages sent by the authenticated user (paged) | |
| `GET` | `/api/v1/messages/received` | List messages received by the authenticated user (paged) | |
| `POST` | `/api/v1/message` | Send a new message | `MessageDTO` |
| `DELETE` | `/api/v1/message/{messageId}` | Delete a message | |

**Pagination** — `GET /api/v1/messages/sent` and `GET /api/v1/messages/received` both return a Spring Data `Page<MessageDTO>` envelope. Use the `page` and `size` query parameters to control pagination (default: page 0, size 20):

```
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

User identity is read from the `x-auth-request-email` header injected by `oauth2-proxy` at the ingress.

Swagger UI: `http://localhost:5553/swagger-ui/index.html` (local) or
`https://skycloud.luksarna.com/msg/swagger-ui/index.html` (cluster).

---

### Architecture

Uses hexagonal (ports-and-adapters):

- `domain/model`, `domain/ports`, `domain/exception`: core domain.
- `adapters/api`: REST controller. Reads sender identity from request headers.
- `adapters/dto`: wire DTOs.
- `config`, `config/propertyBind`: Spring wiring and bound properties.

Plain Spring MVC stack (`spring-boot-starter-web`). There is no Kafka dependency in this service. Do not add it
without a deliberate design decision.

Schema versioning via Flyway. Migrations in [src/main/resources/db/migration/](src/main/resources/db/migration/).

---

### Key environment variables

| Variable | Default | Notes |
|---|---|---|
| `MESSAGE_PORT` | `5553` | Service port |
| `MYSQL_USER` | (required) | DB username |
| `MYSQL_PASS` | (required) | DB password |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://host.docker.internal:3306/sky` | JDBC URL |

---

### Testing

Run from the repo root:

```bash
./gradlew :sky-message:test
```

Integration tests use Testcontainers MySQL (no Kafka container). Docker must be running.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Root README: full platform overview, build, deployment |
| [AGENTS.md](AGENTS.md) | Module-local agent/coding conventions |
| [src/main/resources/db/migration/](src/main/resources/db/migration/) | Flyway SQL migrations |
