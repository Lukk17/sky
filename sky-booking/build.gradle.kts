plugins {
    id("sky.spring-service-conventions")
}

version = "1.0.2"
description = "sky-booking"

dependencies {
    implementation(project(":sky-common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    runtimeOnly(libs.postgresql)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.spring.boot.starter.aop)

    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)

    // JAX-B for Hibernate/Hikari compatibility on JDK 9+
    implementation(libs.bundles.jaxb)
}
