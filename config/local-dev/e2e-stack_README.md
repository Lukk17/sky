# Self-contained end-to-end stack

How to run the whole platform, its database, its identity provider, its broker and its object store from one
Compose file, and how to run the Bruno collection against it. This is what the `e2e-collection` job in
[.github/workflows/ci.yaml](../../.github/workflows/ci.yaml) does on every pull request.

This document owns [config/docker/docker-compose.ci.yaml](../docker/docker-compose.ci.yaml) and the `ci` Bruno
environment. For the day to day developer stack go to [config/local-dev/local_README.md](local_README.md), which
owns [config/docker/docker-compose.yaml](../docker/docker-compose.yaml) and the host installed PostgreSQL,
Keycloak and object store it borrows.

Every command runs from the repository root unless the prose says otherwise.

---

### Why a second compose file

The developer stack is not self contained. It brings up Kafka and the five services and expects PostgreSQL,
Keycloak and the object store to already be running on the host, borrowed from the `local-dev` project in the
neighbouring `InstallationHelper` checkout. That is the right trade for a machine where those three are always
up. A CI runner has none of them, and neither does a new contributor.

So this file brings up everything, from public images pinned to an exact version and to a digest, and the Bruno
collection runs inside the same network. Nothing in the run path touches the host: no certificate, no hosts file
entry, no host port.

---

### What is in the stack

| Service | Image | Loopback port | Why it is here |
|---|---|---|---|
| `postgres` | `postgres:16.11-alpine` | 15432 | The `sky` database for the three stateful services, plus the `keycloak` database |
| `keycloak` | `quay.io/keycloak/keycloak:26.5.7` | 18080 | Mints the token the collection sends, imports the `sky` realm |
| `floci` | `floci/floci:2.0.1` | 19070 | S3 compatible object store for the offer photo upload |
| `kafka` | `confluentinc/cp-kafka:7.6.0` | none | Events from `sky-offer` and `sky-booking` to `sky-notify` |
| `sky-booking` | built from `sky-booking/docker/Dockerfile` | none | Bookings, port 5555 inside the network |
| `sky-offer` | built from `sky-offer/docker/Dockerfile` | none | Offers and photos, port 5552 inside the network |
| `sky-message` | built from `sky-message/docker/Dockerfile` | none | Messages, port 5553 inside the network |
| `sky-notify` | built from `sky-notify/docker/Dockerfile` | none | Notifications, port 5554 inside the network |
| `sky-gateway` | built from `sky-gateway/docker/Dockerfile` | 15777 | The single entry point the collection calls |
| `bruno` | `node:22.23.2-alpine` | none | Runs `bru run -r --env ci`, started on demand only |

The published ports are bound to `127.0.0.1` and exist for poking at a stack that failed. Nothing in the run path
uses them. Ports 5432, 5552 to 5555, 5777, 9070 and 9092 are deliberately left alone, so this stack cannot collide
with the developer stack or with a local k3d cluster on 5777.

The five service images are tagged `:e2e` rather than `:latest`, so bringing this stack up can never move the tag
the developer stack and the local Helm overlays run.

Development only credentials, all of them already public in this repository: database `postgres` / `local`,
Keycloak console `admin` / `admin`, realm users `lukk` / `test1234`, `owner` / `owner` and `user` / `user`, realm
client `sky-backend` with secret `dev-only-change-in-prod`, object store `root` / `localdev`. None of them exists
in any deployed system.

---

### The issuer

Every service validates the `iss` claim against `OAUTH2_ISSUER_URI`, and Keycloak stamps `iss` from its configured
hostname, so the two strings have to be identical or every authenticated call returns 401. The developer stack
solves it with `https://keycloak.test:9443`, a hosts file entry and a certificate from a local authority baked
into each image. A runner has no such hostname and no such certificate.

This stack uses plain HTTP on a Compose service name instead:

```text
KC_HOSTNAME=http://keycloak:8080   ->   iss: http://keycloak:8080/realms/sky
OAUTH2_ISSUER_URI=http://keycloak:8080/realms/sky
keycloakUrl=http://keycloak:8080   (docs/api/request/environments/ci.yml)
```

One string, three consumers, and the collection runs on the same network so it resolves `keycloak` to the same
container the services do. That is also why the `bruno` service exists rather than running `bru` on the host: the
presigned photo URL `sky-offer` signs against `http://floci:4566` has to be fetchable, unchanged and unresigned,
from wherever the collection runs.

Plain HTTP is safe here and nowhere else. The traffic never leaves a throwaway Compose network, the realm carries
no real user, and the alternative costs a certificate authority the runner would have to trust. Keycloak keeps
`sslRequired: external` from the realm file, which permits HTTP from a private address and still demands HTTPS
from a public one.

Confirm the issuer by hand, from inside the network:

```bash
docker run --rm --network sky-e2e-network curlimages/curl:8.11.1 -s http://keycloak:8080/realms/sky/.well-known/openid-configuration
```

The `issuer` field must read `http://keycloak:8080/realms/sky`. The same value comes back through the loopback
port on 18080, because the hostname is fixed rather than taken from the request.

---

### Keycloak, PostgreSQL and the realm

Keycloak waits for PostgreSQL on a health check, not on a sleep:

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d sky"]

keycloak:
  depends_on:
    postgres:
      condition: service_healthy
```

Keycloak gets its own database, `keycloak`, on the same server as `sky`. Not a schema inside `sky`: Keycloak owns
around ninety tables and its own Liquibase history, the three services own `public` through Flyway, and neither
should be able to see the other's migration state. The database and its role are created by an inline Compose
config mounted into `/docker-entrypoint-initdb.d/`, which the PostgreSQL entrypoint runs once, before Keycloak is
allowed to connect. Keycloak creates its own schema on first boot.

The realm is mounted, never copied:

```yaml
volumes:
  - ../k8s/helm/infra/keycloak/files:/opt/keycloak/data/import:ro
```

[config/k8s/helm/infra/keycloak/files/sky-realm.json](../k8s/helm/infra/keycloak/files/sky-realm.json) stays the
only copy of the realm in the repository, so a realm change reaches the cluster, the developer stack and this
stack at once.

One trap worth knowing if you reuse this stack locally. `--import-realm` imports a realm only when it does not
already exist, and the realm lives in the `keycloak` database, which lives in a named volume. Editing the realm
file and running `up` again changes nothing. Tear the stack down with `-v` first, which is what CI does on every
run and what the destroy procedure below does.

---

### Run it

Always pass `-p sky-e2e`. [config/docker/.env](../docker/.env) sets `COMPOSE_PROJECT_NAME=sky`, an environment
variable beats the `name:` field inside a Compose file, and Compose reads that `.env` because it sits next to the
Compose file. Without `-p` this stack adopts the developer stack's project name and Compose starts reconciling its
containers.

Build the images and bring everything up, waiting for every health check:

```bash
docker compose -p sky-e2e -f config/docker/docker-compose.ci.yaml up -d --build --wait
```

Run the collection. This is the gate: `bru` runs every request, exits non-zero when any assertion failed, and
`docker compose run` passes that exit code straight back:

```bash
docker compose -p sky-e2e -f config/docker/docker-compose.ci.yaml run --rm bruno
```

Watch the logs of one service:

```bash
docker compose -p sky-e2e -f config/docker/docker-compose.ci.yaml logs -f sky-offer
```

A healthy run ends with every request green and every assertion green, and `bru` prints the two totals itself. They
are left out of this page on purpose, because the collection grows and a number written here goes stale the next time
it does. Anything less than a clean run is a defect in the stack or in a service, never an assertion to relax.

---

### The Bruno environment

The collection resolves an environment by file name, so
[ci.yml](../../docs/api/request/environments/ci.yml) in
[docs/api/request/environments/](../../docs/api/request/environments/) is selected with `--env ci`.

| Variable | Value | Why it differs from `local` |
|---|---|---|
| `bookingUrl`, `offerUrl`, `messageUrl` | `http://sky-gateway:5777` | The gateway is reached by Compose service name, not through a host port |
| `keycloakUrl` | `http://keycloak:8080` | Plain HTTP on the network, so no `--insecure` and no certificate |

Everything else, the client id, the client secret and the three realm users, is identical to `local` and `k8s`,
because all three read the same realm file.

The `bruno` service mounts the repository read only. A post response script in `auth/get-token.yml` saves the
minted token into `bearerToken`, and Bruno then reports that it could not write the environment file back. That
warning is the read only mount doing its job: the token lives in memory for the rest of the run, which is all the
collection needs, and it can never end up in a committed file.

Running `--env ci` from your own shell or from the Bruno desktop app will not work. None of those hostnames
resolve outside the Compose network.

---

### The CI gate

`e2e-collection` in [.github/workflows/ci.yaml](../../.github/workflows/ci.yaml) runs after `build-and-test`,
builds the five images, brings the stack up with `--wait`, runs the collection, prints container state and logs on
failure, and tears the stack down with `-v` on every outcome including a cancelled run.

It is written to block a merge rather than to report. The three defects that motivated it, a broken photo
presigner, a dead notify route and a double encoded email, all passed every other gate in this repository. The run
is hermetic, with its own database, identity provider, broker and object store and no state carried between runs,
so the usual reason to make an end-to-end job advisory does not apply here.

Blocking the merge button itself needs a branch ruleset naming `e2e-collection` as a required check. This
repository has no ruleset and no branch protection today, so every job here reports a red check and none of them
mechanically blocks a merge. That is a repository setting, not a workflow change.

---

### What it costs

Measured on a Windows 11 workstation with Docker Engine 29.3.1 and Compose v5.1.0, which is faster than a hosted
runner. Treat the numbers as the shape of the cost rather than as runner timings.

| Step | Time | How it was measured |
|---|---|---|
| `build`, one service image, cold | 196 s | `sky-offer` on a throwaway BuildKit builder with an empty cache |
| of which the Gradle step | 143 s | the `RUN ... gradle :sky-offer:bootJar` layer |
| of which pulling `gradle:9-jdk25` | 35 s | the base image resolve in the same run |
| `build`, all five images, warm | 86 s | every Gradle layer already in the local BuildKit cache |
| `up --wait`, nine containers, empty volumes | 48 s to 114 s | two clean runs after `down -v`, images already built |
| `run --rm bruno` | 44 s to 53 s | about 32 s of it installing the CLI into the container |
| The collection itself, every request | 1.9 s to 7.6 s | fastest against a warm stack, slowest against a fresh database |

The image build dominates, and the measurement above was taken on 16 cores while a hosted runner has four, so
five cold images are minutes of wall clock rather than seconds. Nothing in this repository makes that cheaper.
Each of the five Dockerfiles runs Gradle inside the build stage against its own BuildKit cache mount, keyed
`gradle-sky-<service>`, so five cold builds resolve the same dependency set five times and share nothing. A
BuildKit cache mount is not exported by any cache backend, so no amount of `cache-to` fixes that: the fix is a
shared dependency layer across the five Dockerfiles, which is a change to files this stack does not own.

Reuse was considered and rejected on the two obvious shapes:

- Reusing the jars `build-and-test` already compiles would need a second, thinner Dockerfile that copies a
  prebuilt jar. It would be much faster and it would test an image nothing ever ships, while the point of the gate
  is to exercise the same recipe `release.yaml` pushes.
- Exporting the BuildKit layer cache to the GitHub Actions cache would help only when a module did not change, it
  needs `mode=max` to reach the Gradle layer inside the build stage, and five multi-stage builds at `mode=max`
  would fill most of the repository's 10 GB cache quota and evict the Gradle cache `build-and-test` restores
  first. That trade makes the earlier job slower to make the later job faster.

The job therefore builds the real images with no cross-run cache, and the 40 minute timeout is sized for five cold
builds on a four core runner rather than for the warm numbers above.

---

### Troubleshooting

Every authenticated request returns 401. The token and the services disagree about the issuer. Compare the `iss`
claim in the token with `OAUTH2_ISSUER_URI` on the service, both of which must read
`http://keycloak:8080/realms/sky`.

`up --wait` fails on `keycloak`. Read its log. A database error means the `keycloak` database was not created,
which happens when the PostgreSQL volume survived from a run that predates the init config. Tear down with `-v`.

The photo upload request passes but the two round trip assertions fail with `presigned URL not fetchable`. The
`floci` container is unhealthy or the collection is not running on the Compose network.

A Compose command reports containers you did not expect, or removes some you needed. The `-p sky-e2e` flag was
missing, so the command ran against the developer stack's project name.

`up` reports a port already allocated. Something on the host holds 15432, 18080, 19070 or 15777. Those four
mappings are for debugging only, so deleting them from your local copy of the file changes nothing in the run
path.

---

### Destroying it

One command removes the containers, the network and all three named volumes, which is the whole stack including
the Keycloak database and the imported realm:

```bash
docker compose -p sky-e2e -f config/docker/docker-compose.ci.yaml down -v --remove-orphans
```

Confirm nothing is left, PowerShell:

```powershell
docker ps -a --filter "label=com.docker.compose.project=sky-e2e" --format "{{.Names}}"
```

Unix shell:

```bash
docker ps -a --filter "label=com.docker.compose.project=sky-e2e" --format "{{.Names}}"
```

Remove the images this stack builds, PowerShell:

```powershell
docker images --filter "reference=sky-*:e2e" --format "{{.ID}}" | ForEach-Object { docker rmi -f $_ }
```

Unix shell:

```bash
docker images --filter "reference=sky-*:e2e" --format "{{.ID}}" | xargs -r docker rmi -f
```

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../README.md) | Platform overview, modules, build, ports |
| [config/local-dev/local_README.md](local_README.md) | The developer stack: host PostgreSQL, Keycloak and object store, Compose and Gradle |
| [config/keycloak/SETUP.md](../keycloak/SETUP.md) | Keycloak realm, import, certificate trust, users, tokens |
| [config/k8s/local_README.md](../k8s/local_README.md) | Local Kubernetes cluster on k3d: create, deploy the charts, verify, tear down |
| [docs/api/README.md](../../docs/api/README.md) | Bruno collection, its environments, and the OpenAPI specs |
