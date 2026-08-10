plugins {
    id("sky.spring-service-conventions")
    id("sky.kafka-conventions")
}

version = "1.0.2"
description = "sky-notify"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(project(":sky-common"))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation("org.springframework.security:spring-security-messaging")

    implementation(libs.gson)

    testImplementation(libs.testcontainers.kafka)
}
