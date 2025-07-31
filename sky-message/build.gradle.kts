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
description = "sky-message"

java {
    sourceCompatibility = JavaVersion.valueOf("${project.extra["javaVersion"]}")
    targetCompatibility = JavaVersion.valueOf("${project.extra["javaVersion"]}")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("com.mysql:mysql-connector-j")

    implementation("com.google.code.gson:gson")

    //    Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${project.extra["openapiVersion"]}")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.extra["openapiVersion"]}")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter")

    compileOnly("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    annotationProcessor("org.projectlombok:lombok:${project.extra["lombokVersion"]}")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    //  JAX-B dependencies for JDK 9+ (without hibernate/hikari error)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    implementation("org.glassfish.jaxb:jaxb-runtime")
}


