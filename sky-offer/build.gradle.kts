buildscript {
    apply(from = File("../config/microservicesConfig.gradle.kts"))

    val springBootVersion = "${project.extra["springBootVersion"]}"
    System.setProperty("springBootVersion", springBootVersion)
}

plugins {
    val springBootVersion = System.getProperty("springBootVersion")
    id("org.springframework.boot") version springBootVersion
}

version = "1.0.1"
description = "sky-offer"

java {
    JavaVersion.valueOf("${project.extra["javaVersion"]}")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    val springBootVersion = "${project.extra["springBootVersion"]}"
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-rest:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-devtools:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-webflux:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")

    implementation("mysql:mysql-connector-java:${project.extra["mysqlVersion"]}")
    implementation("com.google.code.gson:gson:${project.extra["gsonVersion"]}")
    implementation("org.springframework.kafka:spring-kafka:${project.extra["kafkaVersion"]}")
//    Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${project.extra["openapiVersion"]}")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.extra["openapiVersion"]}")


    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}"){
        exclude("junit", "junit")
    }
    testImplementation("com.h2database:h2:${project.extra["h2Version"]}")
    testImplementation("org.springframework.security:spring-security-test:${project.extra["springSecurityTestVersion"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.extra["jUnit5Version"]}")
    testImplementation("org.springframework.kafka:spring-kafka-test:${project.extra["kafkaVersion"]}")

    compileOnly("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}")

    annotationProcessor("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springBootVersion}")

    //  JAX-B dependencies for JDK 9+ (without hibernate/hikari error)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:${project.extra["jakartaBindApiVersion"]}")
    implementation("org.glassfish.jaxb:jaxb-runtime:${project.extra["jaxbRuntimeVersion"]}")
}

