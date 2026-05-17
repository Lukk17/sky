plugins {
    id("sky.spring-service-conventions")
}

version = "1.0.2"
description = "sky-notify"

// Convention extension: annotationProcessor → compileOnly so Lombok + the configuration
// processor cooperate cleanly (originally lived in sky-notify's old build script).
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

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.testcontainers.kafka)
}
