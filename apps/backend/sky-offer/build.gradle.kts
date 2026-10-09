plugins {
    id("sky.spring-service-conventions")
    id("sky.web-conventions")
    id("sky.kafka-conventions")
    id("sky.openapi-conventions")
    id("org.openapi.generator")
}

version = "2.0.0"
description = "sky-offer"

skyOpenApi {
    docsPort.set(7972)
}

// OpenAPI code generation from contract
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/../../contracts/sky-api/openapi.yaml")
    outputDir.set("$buildDir/generated/sources/openapi")
    apiPackage.set("com.lukk.sky.offer.generated.api")
    modelPackage.set("com.lukk.sky.offer.generated.model")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "useTags" to "true"
    ))
    skipValidateSpec.set(false)
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/sources/openapi/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

dependencies {
    implementation(project(":sky-common"))

    testImplementation(testFixtures(project(":sky-common")))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)

    implementation(platform(libs.awssdk.bom))
    implementation(libs.awssdk.s3)

    implementation(libs.bundles.jaxb)

    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.boot.restclient)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
}
