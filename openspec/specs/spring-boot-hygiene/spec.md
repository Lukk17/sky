# spring-boot-hygiene Specification

## Purpose
Keeps each service's configuration honest: only the starters actually used on the classpath, no credential defaulted outside the deliberately relaxed local profile, and a cross-origin list that names its origins instead of admitting every one with a wildcard.

## Requirements

### Requirement: Only the starters actually used are on the classpath
No module may declare a Spring Boot starter it does not use. Three rules make that checkable. `spring-boot-starter-data-rest` MUST NOT be declared anywhere. A module on the servlet stack MUST NOT declare `spring-boot-starter-webflux`, which leaves sky-gateway as the one reactive module, and it is reactive through `spring-cloud-starter-gateway-server-webflux` rather than through the Spring Boot starter, because Spring Cloud Gateway requires Netty and breaks when Tomcat reaches the classpath. Exactly one springdoc-openapi UI starter MUST be on the classpath of a module that serves a REST API and none MUST be on the classpath of a module that does not, which is achieved by declaring it once in the shared web convention plugin rather than per module, so the three REST services get a Swagger UI by applying that plugin and sky-notify and sky-gateway get none by not applying it.

#### Scenario: Auditing dependencies
- **WHEN** a contributor runs `./gradlew :sky-booking:dependencies`, and the same for the other modules
- **THEN** `spring-boot-starter-data-rest` appears nowhere, `spring-boot-starter-webflux` appears in no servlet-stack module, and exactly one springdoc starter appears for each of the three REST services and none for the other modules

### Requirement: No default credentials in committed config outside the local profile
No `application.yaml`, and no profile-specific configuration file beside it, may carry a default value for a credential, a secret, a password or a username that forms part of one. The environment variable reference MUST stand alone, as `${POSTGRES_PASSWORD}` does, so a missing value cannot fall back to something a deployment did not choose, and a missing credential MUST fail startup loudly rather than surfacing later as a connection failure that names no property. Exactly one exemption exists, and it is the `local` profile: a configuration file activated only by that profile MAY default a development credential. The reason is part of the rule rather than something to rediscover. This project is for local use, its development credentials are deliberately committed so that a fresh checkout runs with no setup, and the `local` profile is already this repository's one deliberately relaxed branch, because the same profile installs a JWT decoder that verifies neither signature, issuer nor expiry, and the gateway's `local` chain permits every exchange. A defaulted development password is the smaller of the relaxations already accepted under that name. Three boundaries keep the exemption where it is and none of them is negotiable. It MUST reach only a file whose own name carries the `local` profile, so a file with no profile in its name is never covered, whatever profile is active when it loads. It MUST NOT extend to any other profile, named or composed. And the `local` profile MUST NOT be activated in a deployed environment, because an exemption attached to a file is worth nothing if that file can be loaded in the cluster. A value defaulted under the exemption MUST be a development value that grants nothing beyond a developer's own machine.

#### Scenario: Starting a service without required env vars
- **WHEN** a developer starts sky-booking on the default profile without setting `POSTGRES_PASSWORD`
- **THEN** the service fails to start with a clear message naming the unresolved property, and it does not silently fall back to a hardcoded default and does not carry the unresolved placeholder forward as if it were the password

#### Scenario: Auditing committed configuration for a credential default
- **WHEN** an operator reads every `application*.yaml` in every module and lists each credential, secret, password or username that carries a default value after the colon inside `${...}`
- **THEN** every one of them sits in a file whose name carries the `local` profile, and no file without a profile in its name carries any such default

### Requirement: Cross-origin configuration never allows every origin
No cross-origin configuration this repository ships for a service, or for the ingress in front of one, may allow every origin. Each allowed-origin value MUST be an explicit list of named origins, MUST NOT be a wildcard, and MUST NOT be written as an origin pattern that a wildcard would satisfy. The rule MUST cover every place such a value is configured rather than only the ones that look like properties: the `sky.crossOrigin.allowed` value of each REST service together with the `ACCESS_CONTROL_ALLOW_ORIGIN` environment variable that overrides it, the `sky.crossOrigin.allowed` value of sky-notify, which the STOMP endpoints read for the WebSocket handshake and which was a list compiled into `WebSocketConfig` until it was bound like the other three, and the `cors-allow-origin` annotation on any ingress fronting a service. Which named origins the list holds is deliberately left open, and the committed default MAY name development origins alongside the production frontend, as all four services do today. That permission is deliberate, not tolerated: the earlier form of this rule required the default to name the production frontend alone and pushed localhost origins into a profile overlay, and no such overlay has ever existed here, so the rule could only have been satisfied by adding configuration nobody wanted and by leaving a local run unable to call its own API. The wildcard is the part that actually matters, because it is the only value that turns the allow-list from a decision into an absence of one, and forbidding it is the part that already holds everywhere.

#### Scenario: Default-profile CORS
- **WHEN** the application boots without an `ACCESS_CONTROL_ALLOW_ORIGIN` environment variable
- **THEN** the configured cross-origin value is the committed default, which names the production frontend and the development origins explicitly, and it is not a wildcard

#### Scenario: Auditing every cross-origin value for a wildcard
- **WHEN** an operator lists every cross-origin value this repository ships for a service or for an ingress in front of one, covering all four services and the ingress annotations in the Helm values
- **THEN** none of them is a wildcard or an origin pattern a wildcard would satisfy, and each one names its origins explicitly
