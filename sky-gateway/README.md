# sky-gateway

Local-development edge proxy for the Sky platform, built on Spring Cloud Gateway.

No Helm chart deploys it and no cluster runs it. In a cluster the same job is done by nginx-ingress rewrite annotations plus oauth2-proxy. This module gives you the same URL surface on `http://localhost:5777` so you never have to remember which service owns which port.

The release pipeline does still build and push `lukk17/sky-gateway:v<version>` with the four service images, so a Compose stack on another host can pull the gateway instead of building it. The chart-pinning step skips it, because there is no chart to pin.

The Spring Cloud release train is pinned in [gradle/libs.versions.toml](../gradle/libs.versions.toml), which is where to bump it.

---

### Routes

| Incoming path | Upstream variable | Default upstream | Rewritten to |
|---|---|---|---|
| `/offer/api/**` | `OFFER_URI` | `http://localhost:5552` | `/api/v1/{remainder}` |
| `/booking/api/**` | `BOOKING_URI` | `http://localhost:5555` | `/api/v1/{remainder}` |
| `/msg/api/**` | `MESSAGE_URI` | `http://localhost:5553` | `/api/v1/{remainder}` |
| `/notifyWebsocket/**` | `NOTIFY_URI` | `http://localhost:5554` | passthrough, no rewrite |

The routes are declared in [src/main/resources/application.yaml](src/main/resources/application.yaml). The three API routes mirror the production nginx rewrite annotations exactly, so a request that works here works against the cluster with only the host changed. The fourth mirrors the `sky-notify` Ingress, which also passes `/notifyWebsocket` through without a rewrite, see below.

The notification route predicate is `/notifyWebsocket/**` because that is the exact path `sky-notify` registers its STOMP endpoint on, so a WebSocket client connects through the gateway at `ws://localhost:5777/notifyWebsocket`. Two tests in [src/test/java/com/lukk/sky/gateway/config/SecurityConfigLocalProfileTest.java](src/test/java/com/lukk/sky/gateway/config/SecurityConfigLocalProfileTest.java) pin it: one asserts `/notifyWebsocket/info` reaches the route, the other asserts the old `/notify/**` prefix now matches nothing and returns 404.

The cluster matches. `sky-notify` ships an Ingress in its Helm chart on the same `/notifyWebsocket` prefix with no rewrite, so a browser reaches the WebSocket there too, and this gateway route is the Compose and bare-Gradle equivalent of it rather than a local-only workaround.

Inside Docker Compose the upstream URIs are overridden to Docker service names, so the gateway resolves each service on the `sky-network` bridge:

```text
BOOKING_URI=http://sky-booking:5555
OFFER_URI=http://sky-offer:5552
MESSAGE_URI=http://sky-message:5553
NOTIFY_URI=http://sky-notify:5554
```

---

### Running it

No authentication, under the `local` profile. The gateway forwards everything and validates nothing, which is what you want while working on one service. The profile is not optional here: leave it off and the OIDC chain activates instead, and startup fails on the unresolved `KEYCLOAK_ISSUER_URI` placeholder.

Unix shell:

```bash
./gradlew :sky-gateway:bootRun --args='--spring.profiles.active=local'
```

PowerShell:

```powershell
.\gradlew.bat :sky-gateway:bootRun --args='--spring.profiles.active=local'
```

Any profile other than `local`, including no profile at all, turns on the Keycloak OIDC login flow and the `TokenRelay` filter, so the gateway obtains the token and forwards it downstream. It needs `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_CLIENT_ID`, and `KEYCLOAK_CLIENT_SECRET`.

Unix shell:

```bash
./gradlew :sky-gateway:bootRun
```

PowerShell:

```powershell
.\gradlew.bat :sky-gateway:bootRun
```

With the whole stack in Docker Compose:

```bash
docker compose -f config/docker/docker-compose.yaml up --build -d
```

[config/docker/docker-compose.yaml](../config/docker/docker-compose.yaml) sets `SPRING_PROFILES_ACTIVE=local` on this service, so the compose gateway runs the permit-all chain and needs no Keycloak variables. The gateway listens on 5777, and the individual service ports stay published for direct calls.

---

### Building the image

The Dockerfile copies the root build files, `buildSrc`, `sky-common`, and this module, so the build context is the repository root and not the module directory. Every sky image carries two tags, the version and `latest`, so apply both from the one build. A local tag carries no leading `v`, a published `lukk17/sky-gateway` tag does. [config/docker/docker-compose.yaml](../config/docker/docker-compose.yaml) does the same thing for you.

```bash
docker build . -f sky-gateway/docker/Dockerfile -t sky-gateway:2.0.0 -t sky-gateway:latest
```

---

### Configuration

Everything tuneable is in [src/main/resources/application.yaml](src/main/resources/application.yaml). The OIDC client registration and the token-relay copies of the four routes live in a second document below the `---` separator at the bottom of that file, selected by `spring.config.activate.on-profile: "!local"`, so they apply under every profile except `local`.

| Variable | Default | Purpose |
|---|---|---|
| `GATEWAY_PORT` | `5777` | Port the gateway listens on |
| `OFFER_URI` | `http://localhost:5552` | sky-offer upstream |
| `BOOKING_URI` | `http://localhost:5555` | sky-booking upstream |
| `MESSAGE_URI` | `http://localhost:5553` | sky-message upstream |
| `NOTIFY_URI` | `http://localhost:5554` | sky-notify upstream |
| `GATEWAY_DEBUG` | `INFO` | Log level for the gateway's own routing logs |
| `KEYCLOAK_ISSUER_URI` | none | Required under every profile except `local` |
| `KEYCLOAK_CLIENT_ID` | none | Required under every profile except `local` |
| `KEYCLOAK_CLIENT_SECRET` | none | Required under every profile except `local` |

---

### Why this module exists

The production edge is nginx-ingress rewrite annotations plus oauth2-proxy, and neither is practical to run on a laptop without a Kubernetes cluster. This module reproduces the URL surface (`/offer/api/**`, `/booking/api/**`, `/msg/api/**`) without one, so the Bruno collection and the frontend work against `http://localhost:5777` whether the backend is running from Gradle, from Docker Compose, or in a local cluster.

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Platform overview, modules, build, ports |
| [AGENTS.md](AGENTS.md) | Module-local agent and coding conventions |
| [../config/local-dev/local_README.md](../config/local-dev/local_README.md) | Running the platform locally, with and without Compose |
| [../docs/api/README.md](../docs/api/README.md) | Bruno collection, which targets this gateway by default |
