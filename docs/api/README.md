# Sky API docs

Two sets of artefacts live here:

- [request/](request/) — Bruno collection (OpenCollection YAML) for hands-on HTTP testing
- [openapi/](openapi/) — OpenAPI 3.1 specs for code generation and formal reference

---

### Bruno collection

[Bruno](https://www.usebruno.com/) is a Git-native API client. The collection lives in
[request/](request/) and can be opened from Bruno's "Open Collection" dialog by pointing
at that folder.

The collection uses the [OpenCollection YAML](https://docs.usebruno.com/opencollection-yaml/overview)
format (`.yml` request and environment files, with [request/opencollection.yml](request/opencollection.yml)
as the collection root), which is the default format in Bruno v3.1 and later. It replaces the
legacy single-file `.bru` format; both still open in Bruno if you are on an older release.

The collection has two environments in [request/environments/](request/environments/):

| Environment | `baseUrl` |
|---|---|
| `local` | `http://localhost:8080` (Spring Cloud Gateway) |
| `prod` | `https://skycloud.luksarna.com` |

Both environments have a `bearerToken` variable that you fill in before running protected
requests. See the next section for how to mint one.

---

### Minting a Keycloak token

The Sky services are Keycloak JWT resource servers. Every token is issued by the realm at
`https://keycloak.luksarna.com/realms/sky`. Use the password grant for interactive testing:

```bash
curl -s -X POST \
  "https://keycloak.luksarna.com/realms/sky/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=<your-client-id>" \
  -d "username=<user@example.com>" \
  -d "password=<password>"
```

Copy the `access_token` from the JSON response and paste it into the `bearerToken`
environment variable inside Bruno. Tokens are short-lived (typically 5 minutes). When you
receive a `401`, re-run the curl above to get a fresh token.

For automated scripts or CI, use the client-credentials grant instead:

```bash
curl -s -X POST \
  "https://keycloak.luksarna.com/realms/sky/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=<client-id>" \
  -d "client_secret=<client-secret>"
```

---

### Gateway routing

Locally, sky-gateway (port 8080) strips the service prefix and rewrites the path before
forwarding to the downstream service. Production uses the same mapping via nginx-ingress.

| Gateway prefix | Downstream service | Direct port |
|---|---|---|
| `/booking/api/**` | sky-booking | 5555 |
| `/offer/api/**` | sky-offer | 5552 |
| `/msg/api/**` | sky-message | 5553 |

The Bruno collection uses `{{baseUrl}}/booking/api/...` etc. so requests work against
both the gateway (local or prod) and directly against a service when you change `baseUrl`.

---

### Public vs. protected endpoints

sky-offer has two public endpoints that work without a bearer token:

- `GET {{baseUrl}}/offer/api/offers` — list all offers
- `POST {{baseUrl}}/offer/api/search` — keyword search

Every other endpoint across all three services requires `Authorization: Bearer <token>`.
The sky-booking and sky-message services have no public endpoints at all.

---

### Live Swagger UI

Each service exposes springdoc-generated Swagger UI at `/swagger-ui/index.html` on its own
port. This is useful when running services directly without the gateway:

- sky-booking: `http://localhost:5555/swagger-ui/index.html`
- sky-offer: `http://localhost:5552/swagger-ui/index.html`
- sky-message: `http://localhost:5553/swagger-ui/index.html`

The raw OpenAPI JSON is at `/v3/api-docs` on each service.

---

### OpenAPI specs

The three specs in [openapi/](openapi/) document the exact contract derived from the
controller and DTO source files. Each spec includes component schemas built from the
Java DTO fields (types accurate to `Long`, `BigDecimal`, `LocalDate`, etc.), the Spring
Data `Page<T>` envelope shape, and the RFC 7807 problem-detail error shape produced by
each service's `GlobalExceptionHandler`.

| Spec file | Service |
|---|---|
| [openapi/sky-booking.openapi.yaml](openapi/sky-booking.openapi.yaml) | sky-booking (port 5555) |
| [openapi/sky-offer.openapi.yaml](openapi/sky-offer.openapi.yaml) | sky-offer (port 5552) |
| [openapi/sky-message.openapi.yaml](openapi/sky-message.openapi.yaml) | sky-message (port 5553) |

To generate a client from a spec:

```bash
openapi-generator-cli generate \
  -i docs/api/openapi/sky-booking.openapi.yaml \
  -g typescript-axios \
  -o generated/sky-booking-client
```
