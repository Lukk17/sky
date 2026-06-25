plugins {
    id("sky.spring-service-conventions")
}

version = "1.0.0"
description = "sky-gateway"

// Spring Cloud Gateway is reactive (WebFlux / Netty). The sky.spring-service-conventions
// plugin adds actuator and test infra but does NOT pull spring-boot-starter-web, so the
// gateway starts on Netty as required. Do NOT add spring-boot-starter-web here.

dependencyManagement {
    imports {
        // Spring Cloud 2025.1.x (Oakwood) targets Spring Boot 4.0.x. Importing the BOM after
        // the Spring Boot BOM (already imported by the convention plugin) lets SC manage its own
        // versions while Spring Boot manages the shared ones.
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    // Core gateway — reactive HTTP proxy + route predicates + filters (WebFlux/Netty).
    implementation(libs.spring.cloud.starter.gateway.server.webflux)

    // OAuth2 client: optional `secure` profile wires token-relay to upstream services.
    // Kept optional here so the gateway starts cleanly with no Keycloak instance running.
    implementation(libs.spring.boot.starter.oauth2.client)

    // Lombok for config properties
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.spring.boot.configuration.processor)
}
