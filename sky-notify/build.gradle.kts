plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("java")
}

group = "com.lukk"
version = "1.0.2"
description = "sky-notify"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.websocket)

    implementation(libs.gson)
    implementation(libs.spring.kafka)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
    }
    testImplementation(libs.h2)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.kafka.test)
}
