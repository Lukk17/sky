# Local development without Kubernetes

How to run the sky backend on your own machine with Gradle or Docker Compose, against host-installed PostgreSQL, Keycloak, and an S3-compatible object store.

This document owns the non-Kubernetes local setup. For a local Kubernetes cluster go to [config/k8s/local_README.md](../k8s/local_README.md) instead, which owns every cluster command and the cluster credentials.

Every command runs from the repository root unless the prose says otherwise.

---

### Endpoints and credentials

Development-only, non-secret, intentionally committed values. Real environments inject secrets through Kubernetes sealed secrets, and none of the values below exist in any deployed system.

| Service | Address | Credentials |
|---|---|---|
| Keycloak | https://keycloak.test:9443 | admin / admin for the console, realm `sky` |
| Keycloak realm users | https://keycloak.test:9443/realms/sky | lukk / test1234, owner / owner, user / user |
| Keycloak client | `sky-backend` | secret `dev-only-change-in-prod` |
| PostgreSQL | localhost:5432 | database `sky`, user postgres, password local |
| Object store S3 API | http://localhost:9070 | root / localdev |
| Object store console | http://localhost:9071 | root / localdev |
| Kafka | localhost:9092 | no authentication |

The object-store password is `localdev` rather than `local` because MinIO refuses to start with a root password shorter than eight characters, and MinIO is still one of the two implementations this page offers:

```text
HINT: MINIO_ROOT_USER length should be at least 3, and MINIO_ROOT_PASSWORD length at least 8 characters
```

`keycloak.test` must resolve to `127.0.0.1` in your hosts file (`/etc/hosts`, or `C:\Windows\System32\drivers\etc\hosts` on Windows). The four services default to `OAUTH2_ISSUER_URI=https://keycloak.test:9443/realms/sky`, so a bare `./gradlew :sky-offer:bootRun` works without exporting anything.

---

### 1. Start Keycloak

Keycloak serves HTTPS on host port 9443 using the development keypair the `local-dev` stack generates at `local-dev/auth/certificates/localhost/`, and imports the `sky` realm from the chart's realm file at [config/k8s/helm/infra/keycloak/files/sky-realm.json](../k8s/helm/infra/keycloak/files/sky-realm.json). That file is the only copy of the realm in the repository.

The commands below assume the `InstallationHelper` checkout sits beside this one, so `../InstallationHelper` resolves from the repository root. The private key stays in that project and is never copied here. Only the certificate authority is committed in this repository, at [config/keycloak/certs/localhost-ca.crt](../keycloak/certs/localhost-ca.crt), because a Docker build context cannot read a path outside the repository and the service images need the trust anchor at build time.

PowerShell:

```powershell
docker run -d --name keycloak -p 9443:8443 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -e KC_HOSTNAME=https://keycloak.test:9443 -e KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/tls.crt -e KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/tls.key -v "${PWD}/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.crt:/opt/keycloak/conf/tls.crt:ro" -v "${PWD}/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.key:/opt/keycloak/conf/tls.key:ro" -v "${PWD}/config/k8s/helm/infra/keycloak/files:/opt/keycloak/data/import:ro" quay.io/keycloak/keycloak:26.5.7 start-dev --import-realm
```

Unix shell:

```bash
docker run -d --name keycloak -p 9443:8443 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -e KC_HOSTNAME=https://keycloak.test:9443 -e KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/tls.crt -e KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/tls.key -v "$(pwd)/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.crt:/opt/keycloak/conf/tls.crt:ro" -v "$(pwd)/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.key:/opt/keycloak/conf/tls.key:ro" -v "$(pwd)/config/k8s/helm/infra/keycloak/files:/opt/keycloak/data/import:ro" quay.io/keycloak/keycloak:26.5.7 start-dev --import-realm
```

Confirm the issuer matches what the services expect:

```bash
curl -sk https://keycloak.test:9443/realms/sky/.well-known/openid-configuration
```

The `issuer` field must read `https://keycloak.test:9443/realms/sky`. Realm import, user management, token minting, and the certificate trust step are all in [config/keycloak/SETUP.md](../keycloak/SETUP.md).

---

### 2. Start PostgreSQL

One database named `sky` serves all three stateful services, with every table in the `public` schema. Flyway creates and versions them on service startup, so there is nothing to load by hand.

```bash
docker run -d --name sky-postgres -p 5432:5432 -e POSTGRES_DB=sky -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=local postgres:16-alpine
```

The tag above matches the cluster, where [config/k8s/helm/db/postgres/values.yaml](../k8s/helm/db/postgres/values.yaml) pins `postgres:16-alpine`. The test suite is the one place that runs 17, through Testcontainers, so a schema change has to be valid on both.

Check it answers:

```bash
docker exec sky-postgres pg_isready -U postgres -d sky
```

---

### 3. Start the object store

`sky-offer` stores offer photos here. The only requirement is an S3-compatible endpoint on port 9070. The bucket is created on startup by [sky-offer/src/main/java/com/lukk/sky/offer/config/S3Config.java](../../sky-offer/src/main/java/com/lukk/sky/offer/config/S3Config.java), so no bucket-init step is needed.

Two implementations both satisfy it, and the application cannot tell them apart:

- floci, a local AWS emulator. This is what the shared local-dev stack on this machine runs and what the Helm chart now installs in a cluster, pinned to the same image digest in both places. It ignores the access key and the secret, so `root` and `localdev` pass through unchanged.
- MinIO, a fallback if you want a store that actually enforces credentials. It does check the SigV4 signature, which is why the committed values are a valid MinIO pair, and it is useful precisely when you want that check exercised. Nothing in the cluster runs it any more.

Floci serves the S3 API on container port 4566, and its console is a second container that has to resolve the first by name. Create a user-defined network, because the default bridge gives no name resolution between containers:

```bash
docker network create sky-objectstore
```

```bash
docker run -d --name floci --network sky-objectstore -p 9070:4566 -e FLOCI_STORAGE_MODE=persistent -e FLOCI_STORAGE_PERSISTENT_PATH=/app/data -v floci_data:/app/data floci/floci:2.0.1@sha256:4e451c39c7bb88e3cd4f87e8fc0c25d5b47695a51185d521e2241fa00486e8eb
```

```bash
docker run -d --name floci-ui --network sky-objectstore -p 9071:4500 -e FLOCI_ENDPOINT=http://floci:4566 floci/floci-ui:0.4.0
```

MinIO instead, if you want the credential and signature checks a real store performs. PowerShell:

```powershell
docker run -d --name sky-minio -p 9070:9000 -p 9071:9001 -e MINIO_ROOT_USER=root -e MINIO_ROOT_PASSWORD=localdev minio/minio:RELEASE.2024-11-07T00-52-20Z server /data --console-address ":9001"
```

Unix shell:

```bash
docker run -d --name sky-minio -p 9070:9000 -p 9071:9001 -e MINIO_ROOT_USER=root -e MINIO_ROOT_PASSWORD=localdev minio/minio:RELEASE.2024-11-07T00-52-20Z server /data --console-address ":9001"
```

Check the endpoint answers, whichever you started. A `200` means an S3 service is listening, a `403` means MinIO is listening and rejecting the unsigned request, and a connection error means nothing is there:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:9070/
```

The health path differs between the two, so do not reach for `/minio/health/live` unless you know you started MinIO. floci answers both `/health` and `/_floci/health`, and returns 404 for the MinIO path. The Helm chart and the local-dev compose file both probe `/_floci/health`, which is the path the image's own `HEALTHCHECK` uses.

---

### 4. Trust the Keycloak certificate

The OAuth2 resource server fetches the JWKS over HTTPS, and the JVM rejects the certificate with a PKIX path validation error until the authority that issued it is trusted. Never disable TLS validation in committed code.

Every image in this repository already trusts it, the gateway included: each `docker/Dockerfile` imports [config/keycloak/certs/localhost-ca.crt](../keycloak/certs/localhost-ca.crt) under the alias `local-dev-ca` into the JRE `cacerts` store at build time, next to the public certificate authorities rather than instead of them. Trusting the authority rather than the leaf means a reissued leaf needs no image rebuild. So the whole Docker Compose stack needs nothing extra, and this step is only for a service you start with `./gradlew bootRun` on the host, which uses your own JDK's trust store.

The full procedure, with PowerShell and Unix variants for `openssl` and `keytool`, is in [config/keycloak/SETUP.md](../keycloak/SETUP.md).

---

### Option A, run everything in Docker Compose

[config/docker/docker-compose.yaml](../docker/docker-compose.yaml) starts Kafka, the four services, and the gateway. PostgreSQL, Keycloak, and the object store are not in the file, they must already be running from steps 1 to 3. The compose services reach all three on `host.docker.internal`, so they work against a host install or a separate compose project either way.

There is no pre-step. The images bake the local development certificate authority into their own `cacerts`, and the compose file mounts no truststore and sets no `JAVA_TOOL_OPTIONS` override, so `up --build` is the whole flow. It used to mount one, and that was wrong: `-Djavax.net.ssl.trustStore` replaces the JVM trust store instead of adding to it, so the containers trusted Keycloak and no public authority.

Start the stack:

```bash
docker compose -f config/docker/docker-compose.yaml up --build -d
```

Follow the logs:

```bash
docker compose -f config/docker/docker-compose.yaml logs -f
```

Stop it:

```bash
docker compose -f config/docker/docker-compose.yaml down
```

The gateway answers at `http://localhost:5777` with the same path prefixes the cluster ingress uses, and it is the only sky port published to the host. Ports 5552 to 5555 stay open inside the compose network, where the gateway and the services reach each other by service name, and no service port is dialable from the host. That matches the k3d cluster, which is created with `--port "5777:80@loadbalancer"` and publishes nothing else, so a path that works here works there. To call one service directly, run it with Gradle (option B below), which binds its port on the host.

Give the containers a minute after start: readiness probes have a 60 second start period, and calls before that return errors.

---

### Option B, run one service with Gradle

Useful when you are changing one service and want a fast edit-run loop.

Unix shell:

```bash
./gradlew :sky-offer:bootRun --args='--spring.profiles.active=local'
```

PowerShell:

```powershell
.\gradlew.bat :sky-offer:bootRun --args='--spring.profiles.active=local'
```

The `local` profile turns on the Flyway repeatable demo seed. The datasource, Kafka, S3, and issuer defaults live in each service's `application.yaml`, and the defaults already point at the host addresses in the table above, except for two:

- `POSTGRES_USER` and `POSTGRES_PASSWORD` have no defaults. Export them, or the context fails to start.
- `KAFKA_ADDRESS` defaults to `kafka-service`, the in-cluster hostname. Set it to whatever host runs your broker.

A service run on the host can use the compose broker, but the broker advertises itself as `kafka:9092`, so the host needs `127.0.0.1 kafka` in its hosts file for the client to follow the advertised address after the initial connect.

Start only the broker from compose:

```bash
docker compose -f config/docker/docker-compose.yaml up -d kafka
```

`sky-message` needs no broker at all, it has no Kafka dependency.

---

### Building the images by hand

Compose builds the images for you and tags each one twice from a single build. Build one on its own when you want to push it or import it into a cluster, and carry both tags by hand too: the version tag records what is inside, and `latest` is what the Compose stack runs and what the local Helm overlays expect on a cluster node. A local tag carries no leading `v`, unlike a published one, and `SKY_VERSION` overrides the version Compose uses. Every build runs from the repository root, not from the module directory, because the Dockerfile copies `settings.gradle.kts`, `buildSrc`, and `sky-common` alongside the service.

```bash
docker build . -f sky-offer/docker/Dockerfile -t sky-offer:2.0.0 -t sky-offer:latest
```

```bash
docker build . -f sky-booking/docker/Dockerfile -t sky-booking:2.0.0 -t sky-booking:latest
```

```bash
docker build . -f sky-message/docker/Dockerfile -t sky-message:2.0.0 -t sky-message:latest
```

```bash
docker build . -f sky-notify/docker/Dockerfile -t sky-notify:2.0.0 -t sky-notify:latest
```

```bash
docker build . -f sky-gateway/docker/Dockerfile -t sky-gateway:2.0.0 -t sky-gateway:latest
```

To run them outside compose, put them on one network so they can resolve each other by container name:

```bash
docker network create sky-net
```

```bash
docker run -d --name sky-offer --network sky-net -p 5552:5552 sky-offer:latest
```

---

### Troubleshooting

A service exits at startup with `Failed to configure a DataSource`. `POSTGRES_USER` or `POSTGRES_PASSWORD` is unset. Neither has a default in `application.yaml`.

A service logs `PKIX path validation failed`. The Keycloak certificate is not trusted by that JVM. Go back to step 4.

A service starts but every authenticated call returns 401. The token came from a different issuer than the one the service validates against. Compare the `iss` claim in the token with the service's `OAUTH2_ISSUER_URI`. This is the usual symptom of pointing a host-run service at the cluster Keycloak, or the reverse.

Photo upload fails while everything else works. The status says which half to look at. A 503 with `Retry-After: 10` means the store never answered, so check that the container from step 3 is up and listening on port 9070. A 502 means the store answered and refused, so check `S3_ACCESS_KEY`, `S3_SECRET_KEY` and `S3_BUCKET` against what the store actually holds. Either way `sky-offer` logs a warning and boots anyway when the bucket check fails at startup, so the failure only shows up on the photo endpoints.

---

### Clearing

Remove the local infrastructure containers:

```bash
docker rm -f keycloak sky-postgres floci floci-ui
```

Remove the compose stack including its Kafka volume:

```bash
docker compose -f config/docker/docker-compose.yaml down -v
```

List this project's images:

```bash
docker images -a --filter "reference=sky-*"
```

Remove them, PowerShell:

```powershell
docker images -a --filter "reference=sky-*" --format "{{.ID}}" | ForEach-Object { docker rmi -f $_ }
```

Remove them, Unix shell:

```bash
docker images -a --filter "reference=sky-*" --format "{{.ID}}" | xargs -r docker rmi -f
```

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../README.md) | Platform overview, modules, build, ports |
| [config/local-dev/e2e-stack_README.md](e2e-stack_README.md) | The self-contained end-to-end stack and the Bruno gate in CI |
| [config/k8s/local_README.md](../k8s/local_README.md) | Local Kubernetes cluster on k3d: bring-up, verification, teardown |
| [config/k8s/helm/helm_README.md](../k8s/helm/helm_README.md) | Chart-by-chart reference, secret key inventory, upgrades |
| [config/k8s/_deployment-scripts/deployment_README.md](../k8s/_deployment-scripts/deployment_README.md) | Deploying to the GCP cluster, sealed secrets, deployment scripts |
| [config/k8s/k8s_README.md](../k8s/k8s_README.md) | Operating a running cluster with kubectl |
| [config/keycloak/SETUP.md](../keycloak/SETUP.md) | Keycloak realm, import, certificate trust, users, tokens |
| [docs/api/README.md](../../docs/api/README.md) | Bruno collection and OpenAPI specs |
