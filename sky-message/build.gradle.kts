plugins {
    id("sky.spring-service-conventions")
}

version = "1.0.2"
description = "sky-message"

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.rest)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    runtimeOnly(libs.mysql.connector.j)

    implementation(libs.gson)

    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // JAX-B for Hibernate/Hikari compatibility on JDK 9+
    implementation(libs.bundles.jaxb)
}
