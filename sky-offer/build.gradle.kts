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

    runtimeOnly(libs.mysql.connector.j)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.mysql)

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.kafka.test)

    // JAX-B for Hibernate/Hikari compatibility on JDK 9+
    implementation(libs.bundles.jaxb)
}
