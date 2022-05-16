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
description = "auth-service"
java.sourceCompatibility = JavaVersion.valueOf("${project.extra["javaVersion"]}")

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

dependencies {
    val springVersion = "${project.extra["springVersion"]}"
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-rest:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-web:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-security:${springVersion}")

    implementation("org.springframework.security:spring-security-crypto:5.6.3")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:3.1.2")
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("io.jsonwebtoken:jjwt:0.9.1")

    runtimeOnly("org.springframework.boot:spring-boot-devtools:${springVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springVersion}")
    testImplementation("org.springframework.security:spring-security-test:5.6.3")
    testImplementation("com.h2database:h2:2.1.212")

    compileOnly("org.projectlombok:lombok:1.18.24")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")

//  JAX-B dependencies for JDK 9+ (without hibernate/hikari error)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:2.3.2")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    annotationProcessor("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")
}
