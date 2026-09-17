# Sky API docs

Two sets of artefacts live here:

- [request/](request/), the Bruno collection (OpenCollection YAML) for hands-on HTTP testing
- [openapi/](openapi/), the OpenAPI 3.1 specs for code generation and formal reference

---

### Bruno collection

[Bruno](https://www.usebruno.com/) is a Git-native API client. The collection lives in
[request/](request/) and can be opened from Bruno's "Open Collection" dialog by pointing
at that folder.

The collection uses the [OpenCollection YAML](https://docs.usebruno.com/opencollection-yaml/overview)
format (`.yml` request and environment files, with [request/opencollection.yml](request/opencollection.yml)
as the collection root), which is the default format in Bruno v3.1 and later. It replaces the
legacy single-file `.bru` format. Both still open in Bruno if you are on an older release.

The collection has four environments in [request/environments/](request/environments/):

| File | `--env` value | Name shown in the app | `baseUrl` | Target |
|---|---|---|---|---|
| [request/environments/local.yml](request/environments/local.yml) | `local` | `sky-local` | `http://localhost:5777` | The local Docker Compose or Gradle stack behind sky-gateway |
| [request/environments/k8s.yml](request/environments/k8s.yml) | `k8s` | `k8s` | `http://localhost:5777` | A local in-cluster deployment (k3d, minikube, kind) |
| [request/environments/prod.yml](request/environments/prod.yml) | `prod` | `sky-prod` | `https://skycloud.luksarna.com` | The deployed cluster |
| [request/environments/ci.yml](request/environments/ci.yml) | `ci` | `sky-e2e` | `http://sky-gateway:5777` | The self-contained compose stack, from inside its own network |

The `--env` value is the file name without its extension, which is what the CLI resolves. The name inside
the file is what the Bruno desktop app shows in its environment selector. They do not have to match, and
for `local` and `prod` they deliberately do not.

`local` and `k8s` share a `baseUrl` and differ in `keycloakUrl`: `local` mints tokens from the host
Keycloak, `k8s` from the cluster's own. Picking the wrong one gives you a valid token and a 401 on every
authenticated call, because the issuer will not match what the services validate against. See
[config/k8s/local_README.md](../../config/k8s/local_README.md) for the cluster runbook.

`ci` is the odd one out: every host in it is a Docker Compose service name, so it only resolves from inside the
network [config/docker/docker-compose.ci.yaml](../../config/docker/docker-compose.ci.yaml) creates, and the
`bruno` service in that file is what runs it. See
[config/local-dev/e2e-stack_README.md](../../config/local-dev/e2e-stack_README.md) for that stack and for the CI
gate built on it.

Every environment also carries the realm client and user credentials plus an empty `bearerToken`. You never
fill `bearerToken` by hand, the `auth/get-token.yml` request mints a token and saves it there.

---

### Authentication and the self-driving run

The Sky services are Keycloak JWT resource servers. Tokens are issued by the `sky` realm at the host in the
`keycloakUrl` environment variable (`https://keycloak.test:9443` locally, `https://keycloak.luksarna.com` in prod).

The collection is self-driving: you never copy a token or an id by hand.

1. `auth/get-token.yml` (seq 1) runs the Keycloak password grant and, in a post-response script, saves the
   returned `access_token` into the `bearerToken` environment variable. Every other request sends
   `Authorization: Bearer {{bearerToken}}`, so once this request has run they are all authenticated.
2. `offer/create-offer.yml` saves the new offer id into the runtime variable `offerId`, and the owner lookup, every
   photo request, the edit and the final delete all reference `{{offerId}}`. `booking/create-booking.yml` saves
   `bookingId` for its delete, and `message/send-message.yml` saves `messageId` for its delete.
3. `offer/upload-photo.yml` posts the 400x200 PNG at
   [../../e2e/fixtures/offer-photo.png](../../e2e/fixtures/offer-photo.png) as `multipart/form-data` under the
   `file` field, the part name the controller expects. The path in the request,
   `../../../e2e/fixtures/offer-photo.png`, leaves the collection root, which Bruno allows, so the e2e suite and the
   collection share one copy of the image. A post-response script then fetches the presigned `photoUrl` back with
   `bru.sendRequest` and the request asserts the canary marker `SKY-OFFER-PHOTO-CANARY-4471` is in the stored bytes.
   The script asks for the body hex-encoded and looks for the hex of the marker, because Bruno's default safe
   sandbox passes a response body across as a C string and a PNG truncates at its first NUL byte. If the object
   store is unreachable, the two round-trip assertions report `presigned URL not fetchable: <reason>` and a status
   of 0, so a store outage reads as an outage rather than as a wrong response body.
4. Three more requests close the photo lifecycle, and each one goes back to the store rather than trusting the
   response body. `offer/replace-photo.yml` posts a second image and requires the address of the object it replaced
   to answer 404, which is how a leaked object fails the run. `offer/delete-photo.yml` calls
   `DELETE /offer/api/owner/offers/{{offerId}}/photo`, expects 204, and requires the address it just cleared to be
   gone as well. `offer/restore-photo.yml` uploads once more so the teardown still has a photo to take with it, and
   `cleanup/delete-offer.yml` then requires that last object to be gone too. `offer/edit-offer.yml` sits in the same
   group: it refetches the photo after the edit, which pins the rule that an edit cannot touch the stored object.
   None of them names an object key, because the key belongs to the server and every one of them addresses the photo
   through `{{offerId}}`.

The collection runs in dependency order. The `seq` on each request and on each folder makes the run flow as:
get token, then offer create, the reads, the owner lookup, the photo upload, the edit, then the photo replace,
delete and restore, then booking create, read and delete, then message send, read and delete, and finally the offer
delete as teardown.

---

### Run from the Bruno desktop app (GUI)

This is the normal way to use the collection day to day. The terminal path below is mainly for automation, AI
agents, and the OpenSpec e2e runbooks.

1. Open Bruno, choose "Open Collection", and point it at [request/](request/).
2. In the environment selector (top right), pick `sky-local`, `k8s`, or `sky-prod`.
3. To run single requests, run `auth/get-token.yml` once, then run any other request. The saved `bearerToken`
   and the chained ids (`offerId`, `bookingId`, `messageId`) are reused for the rest of the session.
4. To run the whole flow, open the Collection Runner (right-click the collection, then "Run"). It executes in
   folder and request `seq` order, so it finishes with the `cleanup/` folder as teardown. The GUI Runner follows
   that tree order and does not let you reorder requests ad hoc, which is exactly why teardown lives in its own
   ordered `cleanup/` folder rather than being interleaved with the create and read requests.

---

### Run from the terminal (CLI)

```bash
bru run -r --env local --insecure
```

Switch `--env local` to `--env k8s` for a local in-cluster deployment, or `--env prod` for the deployed stack.
`--insecure` is there because `local` and `k8s` both mint tokens from a Keycloak on the self-signed development
certificate. Drop it for `prod`, which has a real one, and for `ci`, which is plain HTTP on a private network.

`ci` is not run this way. It runs inside the compose network:

```bash
docker compose -p sky-e2e -f config/docker/docker-compose.ci.yaml run --rm bruno
```

Before a prod run, fill `keycloakClientSecret`, `keycloakUsername`, and `keycloakPassword` in
[request/environments/prod.yml](request/environments/prod.yml). The `local` and `k8s` environments already carry the
development realm credentials, taken from
[config/k8s/helm/infra/keycloak/files/sky-realm.json](../../config/k8s/helm/infra/keycloak/files/sky-realm.json).

---

### Mint a token by hand

If you prefer to mint a token by hand, for example to inspect its claims, use the password grant directly:

```bash
curl -s -X POST "https://keycloak.test:9443/realms/sky/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password" -d "client_id=sky-backend" -d "client_secret=dev-only-change-in-prod" -d "username=owner" -d "password=owner"
```

For automated scripts or CI without a user, use the client-credentials grant instead:

```bash
curl -s -X POST "https://keycloak.test:9443/realms/sky/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials" -d "client_id=<client-id>" -d "client_secret=<client-secret>"
```

Tokens live for 300 seconds. Re-run `auth/get-token.yml`, or the curl above, when you start getting `401`.

---

### Gateway routing

Locally, sky-gateway (port 5777) strips the service prefix and rewrites the path before
forwarding to the downstream service. Production uses the same mapping via nginx-ingress.

| Gateway prefix | Downstream service | Direct port | Rewritten to |
|---|---|---|---|
| `/booking/api/**` | sky-booking | 5555 | `/api/v1/{remainder}` |
| `/offer/api/**` | sky-offer | 5552 | `/api/v1/{remainder}` |
| `/msg/api/**` | sky-message | 5553 | `/api/v1/{remainder}` |
| `/notifyWebsocket/**` | sky-notify | 5554 | passthrough, no rewrite |

The collection covers the three REST services. The fourth row is the WebSocket handshake, which Bruno does not drive,
and it is listed so the route table here matches [sky-gateway/README.md](../../sky-gateway/README.md).

The Bruno collection uses `{{baseUrl}}/booking/api/...` etc. so requests work against
both the gateway (local or prod) and directly against a service when you change `baseUrl`.

---

### Public vs. protected endpoints

sky-offer has two public endpoints that work without a bearer token:

- `GET {{baseUrl}}/offer/api/offers`, list all offers
- `POST {{baseUrl}}/offer/api/search`, keyword search

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

The three specs in [openapi/](openapi/) are generated output. Never edit one by hand, the next build overwrites
it. They are produced by the springdoc Gradle plugin, wired once in the
[sky.openapi-conventions](../../buildSrc/src/main/kotlin/sky.openapi-conventions.gradle.kts) convention plugin and
applied by the three REST service build files. Each spec carries the component schemas springdoc derives from the
Java DTO fields, the Spring Data `Page<T>` envelope shape, and the problem-detail error shape the shared exception
handler in `sky-common` produces, and nothing beyond what the annotations on the controllers declare.

| Spec file | Service | Gradle task | Generation port |
|---|---|---|---|
| [openapi/sky-booking.openapi.yaml](openapi/sky-booking.openapi.yaml) | sky-booking (port 5555) | `:sky-booking:generateOpenApiDocs` | 7971 |
| [openapi/sky-offer.openapi.yaml](openapi/sky-offer.openapi.yaml) | sky-offer (port 5552) | `:sky-offer:generateOpenApiDocs` | 7972 |
| [openapi/sky-message.openapi.yaml](openapi/sky-message.openapi.yaml) | sky-message (port 5553) | `:sky-message:generateOpenApiDocs` | 7973 |

Regenerate all three, from the repository root:

```bash
./gradlew generateOpenApiDocs
```

```powershell
.\gradlew.bat generateOpenApiDocs
```

`build` depends on `generateOpenApiDocs` in each of the three services, so a full build refreshes the specs and a
stale spec shows up as a dirty working tree. Skip generation when you do not want it:

```bash
./gradlew build -x generateOpenApiDocs
```

```powershell
.\gradlew.bat build -x generateOpenApiDocs
```

Generation forks the service under the `local,openapi` profile pair on the port in the table above. The `openapi`
profile in each service's `src/main/resources/application-openapi.yaml` is what makes that fork need nothing
running: it points the datasource and the Kafka and object-store endpoints at a dead port, turns Flyway off, names
the Hibernate dialect and switches off JDBC metadata access at boot, and tells HikariCP not to open a connection at
startup. The `local` half supplies the unverified JWT decoder, so no Keycloak is needed either. That profile also
moves the springdoc document path under `/v3/api-docs/generated`, because the shared permit-list in `sky-common`
opens `/v3/api-docs/**` and the grouped YAML document otherwise answers on `/v3/api-docs.yaml/public`, which that
pattern does not match.

To generate a client from a spec:

```bash
openapi-generator-cli generate -i docs/api/openapi/sky-booking.openapi.yaml -g typescript-axios -o generated/sky-booking-client
```
