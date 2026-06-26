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
| `local` | `http://localhost:5777` (Spring Cloud Gateway) |
| `prod` | `https://skycloud.luksarna.com` |

Both environments also define a `keycloakUrl` (the realm issuer host) plus the realm client and user
credentials, and a `bearerToken`. You do not fill `bearerToken` by hand: the `auth/get-token.yml` request
mints a token and saves it there automatically. See the next section.

---

### Authentication and the self-driving run

The Sky services are Keycloak JWT resource servers. Tokens are issued by the `sky` realm at the host in the
`keycloakUrl` environment variable (`https://keycloak.test:9443` locally, `https://keycloak.luksarna.com` in prod).

The collection is self-driving: you never copy a token or an id by hand.

1. `auth/get-token.yml` (seq 1) runs the Keycloak password grant and, in a post-response script, saves the
   returned `access_token` into the `bearerToken` environment variable. Every other request sends
   `Authorization: Bearer {{bearerToken}}`, so once this request has run they are all authenticated.
2. `offer/create-offer.yml` saves the new offer id into the runtime variable `offerId`; the owner lookup, photo
   upload, edit and the final delete all reference `{{offerId}}`. `booking/create-booking.yml` saves `bookingId`
   for its delete, and `message/send-message.yml` saves `messageId` for its delete.
3. `offer/upload-photo.yml` posts the 1x1 image at [request/sample.png](request/sample.png) as
   `multipart/form-data` under the `file` field, the part name the controller expects.

Run the whole collection in dependency order with the Bruno CLI. The `seq` on each request and on each folder
makes the run flow as: get token, then offer create, reads, owner lookup, photo upload and edit, then booking
create, read and delete, then message send, read and delete, and finally the offer delete as teardown.

```bash
bru run -r --env local
```

Switch `--env local` to `--env prod` for the deployed stack. Before a prod run, fill `keycloakClientSecret`,
`keycloakUsername` and `keycloakPassword` in [request/environments/prod.yml](request/environments/prod.yml); the
local environment already carries the dev realm credentials taken from `config/keycloak/sky-realm.json`.

In the Bruno desktop app, select the `local` environment, run `auth/get-token.yml` once, then run any other
request; the saved `bearerToken` and the chained ids are reused for the rest of the session.

If you prefer to mint a token by hand, for example to inspect its claims, use the password grant directly:

```bash
curl -s -X POST "https://keycloak.test:9443/realms/sky/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password" -d "client_id=sky-backend" -d "client_secret=dev-only-change-in-prod" -d "username=owner" -d "password=owner"
```

For automated scripts or CI without a user, use the client-credentials grant instead:

```bash
curl -s -X POST "https://keycloak.test:9443/realms/sky/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials" -d "client_id=<client-id>" -d "client_secret=<client-secret>"
```

Tokens are short-lived (typically 5 minutes); re-run `auth/get-token.yml` (or the curl above) when you get a `401`.

---

### Gateway routing

Locally, sky-gateway (port 5777) strips the service prefix and rewrites the path before
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
