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

val contractFile = rootDir.parentFile.parentFile.resolve("docs/api/contract/openapi.yaml")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(contractFile.toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/contract").get().asFile.absolutePath)
    templateDir.set(rootDir.resolve("openapi-templates"))
    apiPackage.set("com.lukk.sky.offer.adapters.inbound.api.contract")
    modelPackage.set("com.lukk.sky.offer.adapters.dto.contract")
    globalProperties.set(
        mapOf(
            "apis" to "Offers,OwnerOffers",
            "models" to "Page,PageableObject,SortObject",
            "supportingFiles" to "false",
        )
    )
    importMappings.set(
        mapOf(
            "ProblemDetail" to "org.springframework.http.ProblemDetail",
            "Pageable" to "org.springframework.data.domain.Pageable",
            "OfferDTO" to "com.lukk.sky.offer.adapters.dto.OfferDTO",
            "OfferEditDTO" to "com.lukk.sky.offer.adapters.dto.OfferEditDTO",
            "PhotoDTO" to "com.lukk.sky.offer.adapters.dto.PhotoDTO",
        )
    )
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot4" to "true",
            "useTags" to "true",
            "skipDefaultInterface" to "true",
            "documentationProvider" to "springdoc",
            "openApiNullable" to "false",
            "useSpringBuiltInValidation" to "true",
            "useJakartaEe" to "true",
            "hideGenerationTimestamp" to "true",
        )
    )
    additionalProperties.set(
        mapOf(
            "omitTypeLevelTags" to "true",
        )
    )
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/contract/src/main/java"))
        }
    }
}

tasks.openApiGenerate {
    inputs.file(contractFile)
    inputs.dir(rootDir.resolve("openapi-templates"))
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
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
