# sky-gateway

Spring Cloud Gateway (2024.0.3) local-development edge proxy for the Sky platform.

In production, routing and authentication are handled by `nginx-ingress` + `oauth2-proxy` on GKE. This module
provides equivalent path-rewriting on `http://localhost:5777` when the stack runs via docker-compose, so developers do
not need to know each service port.

---

## Routes

| Incoming path | Upstream env var | Default upstream | Rewritten path |
|---|---|---|---|
| `/booking/api/**` | `BOOKING_URI` | `http://localhost:5555` | `/api/v1/{remainder}` |
| `/offer/api/**` | `OFFER_URI` | `http://localhost:5552` | `/api/v1/{remainder}` |
| `/msg/api/**` | `MESSAGE_URI` | `http://localhost:5553` | `/api/v1/{remainder}` |
| `/notify/**` | `NOTIFY_URI` | `http://localhost:5554` | passthrough (WebSocket) |

When running inside docker-compose the upstream URIs are overridden to use Docker service names:

- `BOOKING_URI=http://sky-booking:5555`
- `OFFER_URI=http://sky-offer:5552`
- `MESSAGE_URI=http://sky-message:5553`
- `NOTIFY_URI=http://sky-notify:5554`

---

## Running locally

**Mode 1 — no auth (default)**

No Keycloak instance needed. The gateway routes all traffic transparently.

```bash
./gradlew :sky-gateway:bootRun
```

**Mode 2 — with auth (`secure` profile)**

Activates Keycloak OIDC login and `TokenRelay` to forward the bearer token downstream.

Required environment variables:

- `KEYCLOAK_ISSUER_URI` — e.g. `http://localhost:8090/realms/sky`
- `KEYCLOAK_CLIENT_ID` — OAuth2 client ID registered in Keycloak
- `KEYCLOAK_CLIENT_SECRET` — client secret

```bash
./gradlew :sky-gateway:bootRun --args='--spring.profiles.active=secure'
```

**Via docker-compose (all services + gateway)**

```bash
docker compose -f config/docker/docker-compose.yaml up
```

The gateway starts on port 5777. Individual service ports (5552 to 5555) remain exposed.

---

## Configuration

All tuneable settings are in `src/main/resources/application.yaml`. The `secure` profile section is separated by a
`---` document boundary at the bottom of the file and activates only when `spring.profiles.active=secure`.

| Property / env var | Default | Purpose |
|---|---|---|
| `GATEWAY_PORT` | `5777` | Gateway listen port |
| `BOOKING_URI` | `http://localhost:5555` | sky-booking upstream |
| `OFFER_URI` | `http://localhost:5552` | sky-offer upstream |
| `MESSAGE_URI` | `http://localhost:5553` | sky-message upstream |
| `NOTIFY_URI` | `http://localhost:5554` | sky-notify upstream |
| `KEYCLOAK_ISSUER_URI` | (none) | Required only with `secure` profile |
| `KEYCLOAK_CLIENT_ID` | (none) | Required only with `secure` profile |
| `KEYCLOAK_CLIENT_SECRET` | (none) | Required only with `secure` profile |

---

## Why this module exists

The production GKE stack uses `nginx-ingress` rewrite annotations and `oauth2-proxy` for authentication. Neither is
practical to run locally without Minikube. `sky-gateway` gives developers the same URL surface
(`/booking/api/…`, `/offer/api/…`, `/msg/api/…`, `/notify/…`) without Kubernetes, so frontend integration tests and
Postman collections work against `http://localhost:5777` regardless of the environment.
