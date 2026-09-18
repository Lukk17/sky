# Changelog: sky-gateway

All notable changes to this module are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project adheres to
[Semantic Versioning](https://semver.org/). Newest entry on top. The topmost `## [x.y.z]`
version is the current one. The release workflow reads it for the image tag and to guard
against re-publishing an already-released version, so keep it at the top and bump it before
every release. The `version` in `build.gradle.kts` is a cosmetic label the release workflow
does not read. If the two disagree, this file wins for release purposes.

## [2.0.0]

Major, and the first release of this module. It starts at 2.0.0 rather than at 0.1.0 so that one
number describes the whole stack: every module in this repository ships 2.0.0 together, because
they were migrated together and a 1.x client works against none of them. There is no published
`lukk17/sky-gateway` image before this one, so the first release creates the Docker Hub
repository.

### Added
- A Spring Cloud Gateway edge proxy for the local Docker stack, so a developer drives all four
  services through one origin instead of four ports with four CORS entries. It serves on 5777
  (`GATEWAY_PORT`), which keeps it off 8080 and off every service port. Four routes, and none of
  them rewrites anything: the published path is the path the service serves, so the gateway only
  decides which service owns a resource and forwards the request unchanged. `/api/v1/bookings` and
  `/api/v1/user/bookings` go to sky-booking, `/api/v1/offers`, `/api/v1/search` and
  `/api/v1/owner/offers` to sky-offer, `/api/v1/messages` to sky-message, and `/notifyWebsocket/**`
  to sky-notify so the STOMP handshake and its upgrade survive the hop. This works because the three
  REST services own disjoint top-level resources, and it mirrors the cluster ingress paths exactly.
- Two security profiles. The default profile wires an OAuth2 client against Keycloak through
  `KEYCLOAK_ISSUER_URI` and relays the token to the upstream services. The `local` profile
  installs a permit-all chain, so the stack starts and serves with no identity provider running
  at all. The profile is the only thing that decides which, so neither can be reached by
  accident.
- Actuator health with separate liveness and readiness probes, Prometheus metrics, and a startup
  banner, matching the other four services.
- Tests covering both security profiles against a stubbed OIDC provider, so the permit-all chain
  cannot silently become the default and the OIDC chain cannot silently stop requiring a token.
- Six documentation routes, so Swagger UI and the OpenAPI documents are reachable through the one
  origin the rest of the stack already answers on. `/offer/swagger-ui` and `/offer/v3/api-docs` go to
  sky-offer, `/booking/...` to sky-booking and `/msg/...` to sky-message, each stripped of its prefix
  before the hop. Nothing could open a documentation page under Compose before this: the gateway had
  no route that matched one, and the service ports stopped being published when the local stack was
  brought in line with the cluster, where only the ingress is reachable. These six carry a service
  prefix and a rewrite where the four API routes carry neither, because they are the one place the
  disjointness the API routes rest on breaks down: all three services serve the identical
  `/swagger-ui` and `/v3/api-docs`, so nothing but a prefix can say which document a caller wants.
  They mirror the six swagger and api-docs ingresses in the three service charts, same prefixes and
  same targets, so a documentation URL that works here works against the cluster with only the host
  changed. Under the OIDC chain they sit behind the Keycloak login exactly as the cluster puts them
  behind oauth2-proxy.
- `spring.cloud.gateway.server.webflux.trusted-proxies`, scoped to loopback and the three private
  ranges. It reads like unrelated hardening and it is not optional: Gateway 5.0 registers the filter
  that writes `X-Forwarded-Prefix` only when this value is set, and registers a filter that strips
  every `x-forwarded-` header when it is not. springdoc builds the config address and the document
  address it hands the browser out of that header, so with it empty a Swagger UI page loads its shell,
  asks for a document at an address no route can claim, and shows the Swagger demo API instead of ours.

### Changed
- This module is consumed by Docker, not by Kubernetes. There is no Helm chart for it, and there
  is no `deployment.image.tag` anywhere for the release workflow to pin. In the cluster the edge
  is the ingress and the `oauth2-proxy` chart instead. It still participates fully in the release:
  the image is built, tagged `v<version>` and `latest`, and pushed like any other service.
- It runs on Netty through WebFlux, because Spring Cloud Gateway is reactive. Do not add
  `spring-boot-starter-web` to this module: the convention plugin deliberately leaves it out, and
  adding it puts the gateway on a servlet container where its routes do not work.
