plugins {
    id("sky.spring-service-conventions")
}

version = "1.0.2"
description = "sky-offer"

dependencies {
    implementation(project(":sky-common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    runtimeOnly(libs.mysql.connector.j)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.mysql)

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.kafka)

    // JAX-B for Hibernate/Hikari compatibility on JDK 9+
    implementation(libs.bundles.jaxb)
}
