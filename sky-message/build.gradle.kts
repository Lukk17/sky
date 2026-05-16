plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("java")
}

group = "com.lukk"
version = "1.0.2"
description = "sky-message"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.rest)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    developmentOnly(libs.spring.boot.devtools)

    runtimeOnly(libs.mysql.connector.j)

    implementation(libs.gson)

    //    Swagger
    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
    }
    testImplementation(libs.h2)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.spring.boot.configuration.processor)

    //  JAX-B dependencies for JDK 9+ (without hibernate/hikari error)
    implementation(libs.bundles.jaxb)
}
