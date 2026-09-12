plugins {
    id("sky.java-library-conventions")
}

version = "2.0.0"
description = "sky-common: shared wire types, auto-configurations, and web utilities"

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

dependencies {
    // Web (for the shared exception handler, header constants, and CorrelationIdFilter).
    // Kept compileOnly so a non-web consumer never pulls Spring MVC transitively.
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.slf4j:slf4j-api")

    // JSpecify nullness contracts (package-info @NullMarked). Version managed by the Spring Boot BOM.
    // Spring Framework 7 already ships it transitively, declared here because sky-common uses it directly.
    compileOnly("org.jspecify:jspecify")

    // Kafka (for KafkaPayloadModel, KafkaNotificationPublisher). Same reasoning: compileOnly so
    // a non-Kafka service that depends on :sky-common does not pull spring-kafka transitively.
    compileOnly(libs.spring.kafka)
    compileOnly("tools.jackson.core:jackson-databind")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // SpringDoc (for OpenApiAutoConfiguration). compileOnly so sky-notify, which has no
    // Swagger UI, is not forced to pull springdoc onto its classpath.
    compileOnly(libs.springdoc.openapi.starter.webmvc.ui)

    // OAuth2 resource-server support (for AudienceValidator, SecurityUtils, SkySecurityDefaults, and
    // the JWT auto-configurations). compileOnly keeps sky-common from forcing these
    // onto any consumer that does not need JWT validation.
    compileOnly(libs.spring.boot.starter.oauth2.resource.server)

    // Spring Data (for SpringDataExceptionHandler, which turns an unknown sort property into a 400).
    // compileOnly so sky-notify, which has no datastore, never gets spring-data on its classpath.
    // The handler bean is guarded by @ConditionalOnClass so it simply does not exist there.
    compileOnly("org.springframework.data:spring-data-commons")

    // Tests run with the real deps.
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-webmvc")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.jspecify:jspecify")
    testImplementation(libs.spring.kafka)
    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation("org.springframework.data:spring-data-commons")
    // Gson is the oracle for KafkaNotificationPublisher's wire-parity test. Drop it once every
    // module has moved off Gson.
    testImplementation(libs.gson)
    testImplementation(libs.springdoc.openapi.starter.webmvc.ui)
    // junit-platform-launcher must be on the test runtime classpath so the engine and
    // launcher versions align. spring-boot-starter-test pulls it transitively for the
    // service modules. sky-common doesn't use that starter, so we add it directly.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
