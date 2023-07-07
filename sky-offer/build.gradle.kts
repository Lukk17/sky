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
    val springVersion = "${project.extra["springVersion"]}"
    implementation("org.springframework.boot:spring-boot-starter-actuator:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-rest:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-web:${springVersion}")
    implementation("org.springframework.boot:spring-boot-devtools:${springVersion}")

    implementation("mysql:mysql-connector-java:${project.extra["mysqlVersion"]}")
    implementation("com.google.code.gson:gson:${project.extra["gsonVersion"]}")
    implementation("org.springframework.kafka:spring-kafka:${project.extra["kafkaVersion"]}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springVersion}"){
        exclude("junit", "junit")
    }
    testImplementation("com.h2database:h2:${project.extra["h2Version"]}")
    testImplementation("org.springframework.security:spring-security-test:${project.extra["springSecurityTestVersion"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.extra["jUnit5Version"]}")

    compileOnly("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")

    annotationProcessor("org.projectlombok:lombok:${project.extra["lombokVersion"]}")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${springVersion}")

    //  JAX-B dependencies for JDK 9+ (without hibernate/hikari error)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:${project.extra["jakartaBindApiVersion"]}")
    implementation("org.glassfish.jaxb:jaxb-runtime:${project.extra["jaxbRuntimeVersion"]}")
}

