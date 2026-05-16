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
    // Reactive stack retained: BookingService port returns Mono<BookingDTO> and
    // RestClientWebflux uses WebClient. Migration to RestClient is a separate change.
    implementation(libs.spring.boot.starter.webflux)

    runtimeOnly(libs.mysql.connector.j)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.mysql)

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockwebserver)

    // JAX-B for Hibernate/Hikari compatibility on JDK 9+
    implementation(libs.bundles.jaxb)
}
