buildscript {
    apply(from = File("../config/microservicesConfig.gradle.kts"))

    val springBootVersion = "${project.extra["springBootVersion"]}"
    System.setProperty("springBootVersion", springBootVersion)
}

plugins {
    val springBootVersion = System.getProperty("springBootVersion")
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version "1.1.7"
    id("java")
}

version = "1.0.2"
description = "sky-notify"


java {
    sourceCompatibility = JavaVersion.valueOf("${project.extra["javaVersion"]}")
    targetCompatibility = JavaVersion.valueOf("${project.extra["javaVersion"]}")
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
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation("com.google.code.gson:gson")
    implementation("org.springframework.kafka:spring-kafka")

    compileOnly("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    annotationProcessor("org.projectlombok:lombok:${project.extra["lombokVersion"]}")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

