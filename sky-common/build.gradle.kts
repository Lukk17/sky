plugins {
    id("sky.java-library-conventions")
}

version = "1.0.2"
description = "sky-common — shared wire types, auto-configurations, and web utilities"

dependencies {
    // Web (for exception handler base + header constants — kept compileOnly so a non-web
    // consumer never pulls Spring MVC transitively).
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.slf4j:slf4j-api")

    // Kafka (for KafkaProducerAutoConfiguration). Same reasoning: compileOnly so a non-Kafka
    // service that depends on :sky-common does not pull spring-kafka onto its classpath.
    compileOnly(libs.spring.kafka)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Tests run with the real deps.
    testImplementation("org.springframework:spring-web")
    testImplementation(libs.spring.kafka)
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
}
