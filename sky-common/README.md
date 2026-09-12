# sky-common

Shared library for the Sky platform. Not a deployable service: no port, no `main` class, no `bootJar`.

---

### What it is

`sky-common` is a plain Java library that every other module pulls in with `implementation(project(":sky-common"))`: the four services, and `sky-gateway` too, which uses `SecurityPaths` for its own permit-list. It holds the wire types, the security and web auto-configurations, and the constants that would otherwise be copied into five modules and drift apart.

It builds with the `sky.java-library-conventions` plugin rather than `sky.spring-service-conventions`, so there is no Spring Boot plugin, no fat jar, and no service coverage gate.

---

### What it exports

`com.lukk.sky.common.kafka`

- `KafkaPayloadModel`, a record of `payload`, `accessedAt`, and `userInfo`. It is the envelope every Kafka message on this platform uses.
- `KafkaNotificationPublisher`, the shared producer the offer and booking services publish through. It serialises with the Spring-managed Jackson 3 `ObjectMapper` injected into it, not with a JSON library of its own. Gson is gone from every module's `main` source set; the only one left in the repository is a test dependency here, used as the oracle that proves the Jackson output still matches the wire format consumers expect.
- `SkyTopics`, the topic names: `offerTopic-1` and `bookingTopic-1`.

`com.lukk.sky.common.security`

- `SecurityUtils.currentUserEmail()`, the one way a controller learns who is calling. It reads the `email` claim from the validated JWT, so no service parses an identity header.
- `ResourceServerJwtAutoConfiguration`, which registers the role converter and, when `OAUTH2_AUDIENCE` is set, an `AudienceValidator` on top of the default issuer and expiry checks. It deliberately registers no `JwtDecoder`: Spring Boot builds that itself from `spring.security.oauth2.resourceserver.jwt.issuer-uri`, which each service's `application.yaml` fills from `OAUTH2_ISSUER_URI`. `ResourceServerJwtAutoConfigurationTest` asserts the absent decoder, so a future change that adds one will fail rather than quietly shadow Boot's.
- `KeycloakRealmRoleConverter`, which turns Keycloak's `realm_access.roles` into Spring authorities.
- `MethodSecurityAutoConfiguration` and the `@IsUser` meta-annotation for method-level checks.
- `LocalSecurityAutoConfiguration` and `UnverifiedJwtDecoder`, registered only under the `local` Spring profile. The decoder reads a token without verifying its signature, issuer, or expiry, so local runs need no reachable identity provider.
- `SecurityPaths` and `SkySecurityDefaults`, the shared permit-list and defaults.

`com.lukk.sky.common.web`

- `SkyRestExceptionHandler` plus `RestExceptionHandlerAutoConfiguration`, giving every service the same RFC 9457 problem-detail error bodies without a per-service handler.
- `CorrelationIdFilter`, `CorrelationIdClientHttpRequestInterceptor`, and `CorrelationId`, which read or mint `X-Correlation-Id`, put it in the MDC, and pass it on to the next service.
- `ApiVersioningAutoConfiguration`, which configures Spring Framework 7 native API versioning.
- `DateTimeConstants`, the shared zone and formatters.

`com.lukk.sky.common.openapi`

- `OpenApiAutoConfiguration` and `OpenApiSecurityAutoConfiguration`, plus the `@ApiCommonErrorResponses` and `@ApiSecuredErrorResponses` annotations that keep the springdoc documentation consistent across services. The second adds the 401 and 403 responses, so it goes on an endpoint that requires a token and the first goes on one that does not.

`com.lukk.sky.common.config`

- `CommonConfigPropertiesAutoConfiguration` binding the shared server, management, and logging-level property records.

`com.lukk.sky.common.startup`

- `StartupLogConfig`, which emits the startup readiness block as one `log.info` call on readiness, so nothing interleaves into the middle of it.

---

### Dependency discipline

Every Spring dependency here is declared `compileOnly`, on purpose. A consumer that is not a web service must not end up with Spring MVC on its classpath, and a consumer with no broker must not end up with `spring-kafka`, just because it depends on this library. Tests re-add the real dependencies with `testImplementation`.

Do not promote a `compileOnly` dependency to `implementation` without checking every consumer first.

---

### Extending it

1. Put the new type in the package that matches its concern, or add a new one.
2. Keep any new framework dependency `compileOnly` unless every consumer already pulls it.
3. Register a new auto-configuration class in [src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). A class that is not listed there never runs in a consumer.
4. Verify through the consuming services' tests. There is no application to start here.

Build it. Unix shell:

```bash
./gradlew :sky-common:build
```

PowerShell:

```powershell
.\gradlew.bat :sky-common:build
```

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Platform overview, modules, build, ports |
| [AGENTS.md](AGENTS.md) | Module-local agent and coding conventions |
| [../config/local-dev/local_README.md](../config/local-dev/local_README.md) | Running the platform locally |
