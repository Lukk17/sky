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
description = "sky-notify"


java {
    JavaVersion.valueOf("${project.extra["javaVersion"]}")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}


dependencies {
    val springVersion = "${project.extra["springVersion"]}"

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-websocket:3.1.1")

    implementation("com.google.code.gson:gson:${project.extra["gsonVersion"]}")
    implementation("org.springframework.kafka:spring-kafka:${project.extra["kafkaVersion"]}")

    compileOnly("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")

    annotationProcessor("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springVersion}"){
        exclude("junit", "junit")
    }
    testImplementation("com.h2database:h2:${project.extra["h2Version"]}")
    testImplementation("org.springframework.security:spring-security-test:${project.extra["springSecurityTestVersion"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.extra["jUnit5Version"]}")

}

