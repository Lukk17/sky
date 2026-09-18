plugins {
    id("sky.spring-service-conventions")
}

version = "2.0.0"
description = "sky-gateway"

// Spring Cloud Gateway is reactive (WebFlux / Netty). The sky.spring-service-conventions
// plugin adds actuator and test infra but does NOT pull spring-boot-starter-web, so the
// gateway starts on Netty as required. Do NOT add spring-boot-starter-web here.

tasks.jacocoTestCoverageVerification {
    enabled = false
}

dependencyManagement {
    imports {
        // Spring Cloud 2025.1.x (Oakwood) targets Spring Boot 4.0.x. Importing the BOM after
        // the Spring Boot BOM (already imported by the convention plugin) lets SC manage its own
        // versions while Spring Boot manages the shared ones.
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    implementation(project(":sky-common"))

    // Core gateway: reactive HTTP proxy + route predicates + filters (WebFlux/Netty).
    implementation(libs.spring.cloud.starter.gateway.server.webflux)

    // OAuth2 client: the `@Profile("!local")` chain in config/SecurityConfig wires Keycloak OIDC
    // login and the TokenRelay filter. The `local` chain permits every exchange and uses neither.
    implementation(libs.spring.boot.starter.oauth2.client)

    // Lombok for config properties
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.spring.boot.configuration.processor)
}
