buildscript {
    apply(from = File("../config/microservicesConfig.gradle.kts"))

    val springVersion = "${project.extra["springVersion"]}"
    System.setProperty("springVersion", springVersion)
}

plugins {
    val springVersion = System.getProperty("springVersion")
    id("org.springframework.boot") version springVersion
}

version = "0.0.1-SNAPSHOT"
description = "eureka-service"
java.sourceCompatibility = JavaVersion.valueOf("${project.extra["javaVersion"]}")


dependencies {
    val springVersion = "${project.extra["springVersion"]}"
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-web:${springVersion}")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server:3.1.2")
    implementation("com.google.code.gson:gson:2.9.0")

    runtimeOnly("org.springframework.boot:spring-boot-devtools:${springVersion}")

    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")

    annotationProcessor("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")
}