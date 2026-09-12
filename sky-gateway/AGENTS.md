# AGENTS.md: sky-gateway

Module-local guidance for `sky-gateway`. Read the root [AGENTS.md](../AGENTS.md) first for repo-wide stack, build,
architecture, subagent, and OpenSpec rules. This file only adds what is specific to this module.

## What This Module Is

`sky-gateway` is the local-development edge proxy: a Spring Cloud Gateway application that reproduces the path
rewrites the production `nginx-ingress` performs, so the whole stack answers on one port instead of four. It runs on
port 5777 (`GATEWAY_PORT`, default 5777) and fronts the four services under docker-compose. The module version is
not restated here: the topmost `## [x.y.z]` entry in [CHANGELOG.md](CHANGELOG.md) is the current one, and it wins over
the cosmetic `version` in `build.gradle.kts`.

It is not deployed to the cluster: production routing and authentication stay with `nginx-ingress` and
`oauth2-proxy`, so this module has a `docker/Dockerfile` and a compose service but no Helm chart under
`config/k8s/helm/`. The e2e runbooks in `e2e/` and the Bruno `local` environment in `docs/api/request/environments/`
both point at `http://localhost:5777`. See [README.md](README.md) for the human-facing run instructions.

## Architecture

- Build plugin: `sky.spring-service-conventions` only (Spring Boot app, fat `bootJar` named `sky-gateway.jar`,
  JaCoCo report). It applies neither `sky.web-conventions` nor `sky.kafka-conventions`. It does depend on
  `:sky-common`, for `SecurityPaths` in the OIDC chain, so a change to the shared permit-list reaches the gateway too.
- Netty, not Tomcat: Spring Cloud Gateway is reactive (WebFlux on Netty). Adding `spring-boot-starter-web` puts
  Tomcat on the classpath and breaks the gateway, which is why the convention plugin deliberately does not bring it
  in. Do not add it here.
- Spring Cloud BOM: `spring-cloud = 2025.1.2` (Oakwood) from `gradle/libs.versions.toml`, imported in the module
  `dependencyManagement` block after the Spring Boot BOM. The starter is
  `spring-cloud-starter-gateway-server-webflux`, renamed from the old reactive starter in Gateway 5.0.
- Package layout (`com.lukk.sky.gateway`): two classes only, `SkyGatewayApplication` and `config/SecurityConfig`.
  There is no hexagonal structure and no ArchUnit rule here, because the module holds no domain logic: the routing
  table is configuration, not code.
- Routes live in `src/main/resources/application.yaml` under
  `spring.cloud.gateway.server.webflux.routes` and mirror the ingress rewrite annotations:
  `/booking/api/**`, `/offer/api/**` and `/msg/api/**` are rewritten to `/api/v1/{remainder}` on the matching
  service, and `/notifyWebsocket/**` is passed through unrewritten. That predicate is the exact path
  `WebSocketConfig` in `sky-notify` registers its STOMP endpoint on. The earlier `/notify/**` predicate matched
  nothing and the route was dead. `SecurityConfigLocalProfileTest` pins both halves: `/notifyWebsocket/info`
  reaches the route, `/notify/anything` returns 404.
- Production equivalents of the four routes, as the literal nginx annotation values in
  `config/k8s/helm/service/<service>/values.yaml`. Each uses `use-regex: "true"` and captures the remainder in `$2`:

  | Ingress path | `rewrite-target` | Upstream |
  |---|---|---|
  | `/booking/api(/\|$)(.*)` | `/api/v1/$2` | `sky-booking-service:5555` |
  | `/offer/api(/\|$)(.*)` | `/api/v1/$2` | `sky-offer-service:5552` |
  | `/msg/api(/\|$)(.*)` | `/api/v1/$2` | `sky-message-service:5553` |
  | `/notifyWebsocket` | none, passthrough | `sky-notify-service:5554` |

  The last row does have a cluster counterpart now: `config/k8s/helm/service/sky-notify/values.yaml` declares an
  `ingress` block and `templates/ingress.yaml` renders it, serving `/notifyWebsocket` with `pathType: Prefix`, no
  rewrite, no oauth2-proxy auth annotations (the JWT is checked on the STOMP `CONNECT` frame) and
  `proxy-read-timeout` and `proxy-send-timeout` at 3600. So this gateway route mirrors the cluster rather than
  standing in for a missing one, and `kubectl port-forward` is a debugging path rather than the only way in.
  `sky-offer` additionally ships `/offer/api/owner(/|$)(.*)` rewritten to `/api/v1/owner/$2`, which the
  gateway does not reproduce because the generic `/offer/api/**` route already covers it.
- Upstreams are bound under the `sky-gateway` property prefix (`booking-uri`, `offer-uri`, `message-uri`,
  `notify-uri`), defaulting to `http://localhost:5555`, `:5552`, `:5553` and `:5554` in that order and overridden per
  environment through `BOOKING_URI`,
  `OFFER_URI`, `MESSAGE_URI` and `NOTIFY_URI`. Compose sets them to the Docker service names, and also sets
  `SPRING_PROFILES_ACTIVE: local` on this service alone (the four services run on `default`), so the compose gateway
  gets the permit-all chain and needs no Keycloak variables.
- Two security chains, both explicit in `SecurityConfig`, split on one profile rather than on a named pair.
  Spring Boot's reactive auto-configuration secures every exchange when no `SecurityWebFilterChain` bean exists,
  which would make the proxy answer 401, so each branch registers its own chain rather than relying on the default:
  - `@Profile("local")`: every exchange permitted, CSRF, HTTP Basic and form login disabled. The inbound
    `Authorization` header is forwarded untouched and each service validates the token itself. This is the
    opt-in branch, so `local` has to be active explicitly.
  - `@Profile("!local")`: Keycloak OIDC login plus the `TokenRelay` filter on every route. Only
    `SecurityPaths.probes()` from `sky-common` is permitted without a session, which is `/actuator/health/**` and
    `/actuator/info` and nothing else, so `/actuator/prometheus` redirects to the login flow here. Requires
    `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_CLIENT_ID` and `KEYCLOAK_CLIENT_SECRET`. Its configuration is a second
    document in the same `application.yaml`, selected by `spring.config.activate.on-profile: "!local"` below the
    `---` separator. This branch is what a bare `bootRun` with no profile gets, and it fails at startup on the
    unresolved `KEYCLOAK_ISSUER_URI` placeholder unless those three variables are set.
- Actuator: `health`, `info` and `prometheus` exposed, liveness and readiness probes enabled. Exposure is not
  the same as access: under `!local` only the probe paths above are permitted anonymously, so `prometheus` is exposed
  and still requires a session. The Dockerfile health check polls `/actuator/health/liveness` and the compose health
  check polls `/actuator/health/readiness`.

## Testing

Three test classes, run from the repo root with `./gradlew :sky-gateway:test`. No Testcontainers and no database here.
`src/test/resources/application-test.yaml` points the four upstreams at ports nothing listens on (15552 to 15555), so
a proxied request fails at the connection rather than being rejected by security, which is what the routing assertions
rely on.

- `SkyGatewayApplicationTest`, a context-load smoke test on a random port with `{"local", "test"}` active.
- `SecurityConfigLocalProfileTest`, also `{"local", "test"}`: actuator and `/actuator/prometheus` open without
  credentials, a proxied route reaching routing instead of a 401, and the two notify-route predicates.
- `SecurityConfigOidcProfileTest`, `test` only so the `!local` chain applies: unauthenticated `/actuator/prometheus`
  and a proxied route both redirect to `/oauth2/authorization/keycloak`, while health, liveness, readiness and info
  stay open.

The OIDC suite runs against `StubOidcProvider`, a JDK `com.sun.net.httpserver.HttpServer` that serves one discovery
document on a random loopback port and nothing else. `@DynamicPropertySource` feeds its issuer URI plus a dummy client
id and secret in. There is no Keycloak container and no network access: keep it that way, because the only thing
Spring needs at context startup is the discovery document.

The coverage gate is off for this module, and that is the one module-level exemption in the repository.
`jacocoTestCoverageVerification` is disabled in `build.gradle.kts` here, not conditioned on a module name inside the
shared `sky.jacoco-conventions` plugin, so the opt-out sits with the module that needs it. The reason is that the whole
main source set is two classes, `SkyGatewayApplication` and `config/SecurityConfig`, and the plugin's filter excludes
`**/*Application.class` and `**/config/**`, so the measured set is empty. JaCoCo takes no ratio over an empty counter
and reports the rule as satisfied, so leaving the gate on would publish a green tick standing for nothing measured at
all. Keeping `SecurityConfig` in the measured set was the alternative and it does not fix that: the class declares two
profile-selected filter chains and no conditional, so it carries no branch counter, and the 0.90 branch limit would
still sit over an empty counter while only the line limit came alive. What guards this module is the three test classes
above, which cover both filter chains. Delete the exemption as soon as this module owns a class the filter keeps.

## Conventions

- Bump dependency versions in the root `gradle/libs.versions.toml`, never here. That includes the Spring Cloud
  release train, which has to stay on the line that targets the current Spring Boot major.
- Never add `spring-boot-starter-web` (see Netty above), and keep the module free of business logic. Anything that
  needs a domain model belongs in a service, not in the proxy.
- A route change here is only half the change: the production behaviour lives in the
  `nginx.ingress.kubernetes.io/rewrite-target` annotations in `config/k8s/helm/service/<service>/values.yaml`.
  Change both together, or local and production stop agreeing.
- Keep the port at 5777. The e2e runbooks, the Bruno `local` environment, the compose mapping and the Dockerfile
  `EXPOSE` all hardcode it.
