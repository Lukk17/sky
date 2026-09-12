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
  (`GATEWAY_PORT`), which keeps it off 8080 and off every service port. Four routes:
  `/booking/api/**`, `/offer/api/**` and `/msg/api/**` each rewrite to `/api/v1/**` on their
  service, and `/notifyWebsocket/**` passes through to sky-notify unrewritten so the STOMP
  handshake and its upgrade survive the hop.
- Two security profiles. The default profile wires an OAuth2 client against Keycloak through
  `KEYCLOAK_ISSUER_URI` and relays the token to the upstream services. The `local` profile
  installs a permit-all chain, so the stack starts and serves with no identity provider running
  at all. The profile is the only thing that decides which, so neither can be reached by
  accident.
- Actuator health with separate liveness and readiness probes, Prometheus metrics, and a startup
  banner, matching the other four services.
- Tests covering both security profiles against a stubbed OIDC provider, so the permit-all chain
  cannot silently become the default and the OIDC chain cannot silently stop requiring a token.

### Changed
- This module is consumed by Docker, not by Kubernetes. There is no Helm chart for it, and there
  is no `deployment.image.tag` anywhere for the release workflow to pin. In the cluster the edge
  is the ingress and the `oauth2-proxy` chart instead. It still participates fully in the release:
  the image is built, tagged `v<version>` and `latest`, and pushed like any other service.
- It runs on Netty through WebFlux, because Spring Cloud Gateway is reactive. Do not add
  `spring-boot-starter-web` to this module: the convention plugin deliberately leaves it out, and
  adding it puts the gateway on a servlet container where its routes do not work.
