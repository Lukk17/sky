buildscript {
    apply(from = File("../config/microservicesConfig.gradle.kts"))

    val springBootVersion = "${project.extra["springBootVersion"]}"
    System.setProperty("springBootVersion", springBootVersion)
}

plugins {
    val springBootVersion = System.getProperty("springBootVersion")
    id("org.springframework.boot") version springBootVersion
}

version = "1.0.0"
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

tasks.test {
    useJUnitPlatform()
}

dependencies {
    val springBootVersion = "${project.extra["springBootVersion"]}"

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-websocket:${springBootVersion}")

    implementation("com.google.code.gson:gson:${project.extra["gsonVersion"]}")
    implementation("org.springframework.kafka:spring-kafka:${project.extra["kafkaVersion"]}")

    compileOnly("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}")

    annotationProcessor("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}"){
        exclude("junit", "junit")
    }
    testImplementation("com.h2database:h2:${project.extra["h2Version"]}")
    testImplementation("org.springframework.security:spring-security-test:${project.extra["springSecurityTestVersion"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.extra["jUnit5Version"]}")
    testImplementation("org.springframework.kafka:spring-kafka-test:${project.extra["kafkaVersion"]}")

}

