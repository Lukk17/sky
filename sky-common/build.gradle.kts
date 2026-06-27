plugins {
    id("sky.java-library-conventions")
}

version = "1.0.2"
description = "sky-common — shared wire types, auto-configurations, and web utilities"

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

dependencies {
    // Web (for exception handler base, header constants, and CorrelationIdFilter —
    // kept compileOnly so a non-web consumer never pulls Spring MVC transitively).
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.slf4j:slf4j-api")

    // Kafka (for KafkaPayloadModel, KafkaNotificationPublisher). Same reasoning: compileOnly so
    // a non-Kafka service that depends on :sky-common does not pull spring-kafka transitively.
    compileOnly(libs.spring.kafka)
    compileOnly(libs.gson)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // SpringDoc (for OpenApiAutoConfiguration). compileOnly so sky-notify, which has no
    // Swagger UI, is not forced to pull springdoc onto its classpath.
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    // OAuth2 resource-server support (for AudienceValidator, SecurityUtils, and
    // ResourceServerJwtAutoConfiguration). compileOnly keeps sky-common from forcing these
    // onto any consumer that does not need JWT validation.
    compileOnly(libs.spring.boot.starter.oauth2.resource.server)

    // Tests run with the real deps.
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-webmvc")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
    testImplementation(libs.spring.kafka)
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.gson)
    testImplementation(libs.springdoc.openapi.starter.webmvc.ui)
    // junit-platform-launcher must be on the test runtime classpath so the engine and
    // launcher versions align. spring-boot-starter-test pulls it transitively for the
    // service modules; sky-common doesn't use that starter, so we add it directly.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
