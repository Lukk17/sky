plugins {
    id("sky.spring-service-conventions")
}

version = "1.0.2"
description = "sky-booking"

dependencies {
    implementation(project(":sky-common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.rest)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)

    runtimeOnly(libs.mysql.connector.j)

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockwebserver)

    // JAX-B for Hibernate/Hikari compatibility on JDK 9+
    implementation(libs.bundles.jaxb)
}
