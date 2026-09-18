plugins {
    id("sky.spring-service-conventions")
    id("sky.web-conventions")
    id("sky.kafka-conventions")
    id("sky.openapi-conventions")
}

version = "2.0.0"
description = "sky-offer"

skyOpenApi {
    docsPort.set(7972)
}

dependencies {
    implementation(project(":sky-common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)

    implementation(platform(libs.awssdk.bom))
    implementation(libs.awssdk.s3)

    implementation(libs.bundles.jaxb)

    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.boot.restclient)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
}
