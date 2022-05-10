buildscript {
    apply(from = File("../config/configBuild.gradle"))

    val springVersion = "${project.extra["springVersion"]}"
    System.setProperty("springVersion", springVersion)
}

plugins {
    val springVersion = System.getProperty("springVersion")
    id("org.springframework.boot") version springVersion  //"2.6.7"
}

version = "0.0.1-SNAPSHOT"
description = "sky-message"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.2.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.2.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-data-rest:2.2.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-validation:2.2.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-web:2.2.6.RELEASE")
    implementation("mysql:mysql-connector-java:8.0.19")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:2.2.2.RELEASE")
    implementation("org.springframework.boot:spring-boot-devtools:2.2.6.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.2.6.RELEASE")
    testImplementation("com.h2database:h2:1.4.200")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
}


