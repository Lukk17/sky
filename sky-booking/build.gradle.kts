plugins {
    id("sky.spring-service-conventions")
    id("sky.web-conventions")
    id("sky.kafka-conventions")
    id("sky.openapi-conventions")
}

version = "2.0.0"
description = "sky-booking"

skyOpenApi {
    docsPort.set(7971)
}

dependencies {
    implementation(project(":sky-common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)

    implementation(libs.resilience4j.spring.boot4)
    implementation(libs.aspectjweaver)

    implementation(libs.bundles.jaxb)

    testImplementation(libs.spring.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.boot.restclient)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
}
