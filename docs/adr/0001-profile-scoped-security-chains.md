# ADR-0001: Profile-scoped security chains

Status: accepted

Date: 2026-11-06

## Context

Four servlet services and the local gateway select their security filter chain with Spring profile conditions. Naming the profiles a chain serves is the only way the container can show that every profile has exactly one chain. A chain that names none, or names the wrong set, leaves the application on Spring Boot's fallback chain, which secures every exchange and answers 401 rather than failing. The gateway is reactive, so it cannot share the servlet chain. The services implement generated API interfaces, so method security has to proxy classes rather than JDK interfaces. The build-only openapi profile was removed with the per-service OpenAPI document generation, which leaves four profiles the code recognises.

## Decision

The project recognises exactly four Spring profiles: default, local, prod and test. Each security chain and its supporting beans name the profiles they serve explicitly, and no chain uses an exclusion expression.

| Component | Profiles served |
|---|---|
| SecurityAutoConfiguration servlet chain in sky-common | default, local, prod |
| LocalSecurityAutoConfiguration | local |
| Gateway permit-all chain | local |
| Gateway OIDC chain, Keycloak registration, origin filter | default, prod |
| Gateway token-relay application document | default, prod |

The test profile is deliberately absent from every production chain. Servlet tests import SharedTestSecurityConfig from the sky-common test fixtures. It lives in src/testFixtures, outside src/main, and defines its own filter chain, JwtDecoder, JwtAuthenticationConverter, and the userJwt and adminJwt helpers. Gateway tests activate default and test for the OIDC chain and local and test for the local chain.

ActiveProfileGuard runs at startup in the servlet services and in the gateway. It rejects any active profile outside the recognised set with an IllegalStateException. Adding a profile is a two-step change: add it to the chain's @Profile expression first, then to ActiveProfileGuard.RECOGNISED_PROFILES.

Method security is enabled once, in MethodSecurityAutoConfiguration, with @EnableMethodSecurity(proxyTargetClass = true). Controllers implement the generated API interfaces, so a @PreAuthorize on an implementation method only applies when the proxy is a class proxy.

The prod profile is a deployment flag and carries no profile-specific YAML. That is intentional: prod shares the default chain and the default,prod configuration document.

The build-only openapi profile was removed with the per-service OpenAPI document generation, so it is not part of the recognised set.

## Consequences

### Positive

- An unknown profile fails at startup instead of starting silently behind Spring Boot's fallback chain.
- Tests never use the production chain under the test profile, so a test cannot pass on production wiring it does not intend to use.
- The gateway keeps a separate reactive test strategy instead of borrowing the servlet fixture.

### Negative

- Adding a profile requires two coordinated edits: the chain annotation and the guard set.
- prod has no profile-specific YAML, so a prod-only property must go in the default,prod document rather than in a prod document.
- Every servlet test module has to import SharedTestSecurityConfig explicitly.

### Risks

- A new chain that forgets its @Profile is rejected only when the active profile is outside the recognised set. A recognised profile mapped to the wrong chain is not caught. Mitigation: compare a new chain with the profile table above in review.
- The guard and the chain can drift if the two edits happen in the wrong order. Mitigation: the guard's exception message names both edits and the order.

## Rejected Alternatives

### Alternative 1: Exclusion expressions such as !local combined with default

- Pros: shorter annotations, and a new profile inherits a chain without an edit.
- Cons: the served set becomes implicit, and a new profile lands on a chain by accident.
- Why not: an explicit list is what a reviewer can compare with the guard, and it makes the served set readable in one place.

### Alternative 2: A warning instead of stopping startup

- Pros: does not block a boot, which is convenient during an experiment.
- Cons: an application started behind the fallback chain answers 401 to everything, so the warning is easy to miss and the failure surfaces later as a client bug.
- Why not: a silent wrong chain is worse than a refused start.

### Alternative 3: Method security on the profile-scoped chain classes

- Pros: keeps @EnableMethodSecurity next to the chain it belongs to.
- Cons: the profile gate would also gate method security, so a profile without a chain would lose it or need a duplicate annotation, and each chain class would enable it again.
- Why not: one auto-configuration outside the profile gate enables it exactly once for every servlet profile.

### Alternative 4: The test security fixture in src/main

- Pros: every module can reach it without a testFixtures dependency.
- Cons: ships test wiring and an unverified decoder in the production artifact.
- Why not: src/testFixtures keeps it off the runtime classpath of a deployed service.

## References

- [SecurityAutoConfiguration.java](../../apps/backend/sky-common/src/main/java/com/lukk/sky/common/security/SecurityAutoConfiguration.java)
- [LocalSecurityAutoConfiguration.java](../../apps/backend/sky-common/src/main/java/com/lukk/sky/common/security/LocalSecurityAutoConfiguration.java)
- [ActiveProfileGuard.java](../../apps/backend/sky-common/src/main/java/com/lukk/sky/common/security/ActiveProfileGuard.java)
- [MethodSecurityAutoConfiguration.java](../../apps/backend/sky-common/src/main/java/com/lukk/sky/common/security/MethodSecurityAutoConfiguration.java)
- [SharedTestSecurityConfig.java](../../apps/backend/sky-common/src/testFixtures/java/com/lukk/sky/common/test/SharedTestSecurityConfig.java)
- [SecurityConfig.java](../../apps/backend/sky-gateway/src/main/java/com/lukk/sky/gateway/config/SecurityConfig.java)
- [KeycloakClientRegistrationConfig.java](../../apps/backend/sky-gateway/src/main/java/com/lukk/sky/gateway/config/KeycloakClientRegistrationConfig.java)
- [OriginCheckWebFilter.java](../../apps/backend/sky-gateway/src/main/java/com/lukk/sky/gateway/config/OriginCheckWebFilter.java)
- [application.yaml](../../apps/backend/sky-gateway/src/main/resources/application.yaml)
- [SecurityConfigLocalProfileTest.java](../../apps/backend/sky-gateway/src/test/java/com/lukk/sky/gateway/config/SecurityConfigLocalProfileTest.java)
- [SecurityConfigOidcProfileTest.java](../../apps/backend/sky-gateway/src/test/java/com/lukk/sky/gateway/config/SecurityConfigOidcProfileTest.java)
- [ActiveProfileGuardTest.java](../../apps/backend/sky-common/src/test/java/com/lukk/sky/common/security/ActiveProfileGuardTest.java)
