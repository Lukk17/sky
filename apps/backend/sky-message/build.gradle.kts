plugins {
    id("sky.spring-service-conventions")
    id("sky.web-conventions")
    id("sky.openapi-conventions")
    id("org.openapi.generator")
}

version = "2.0.0"
description = "sky-message"

skyOpenApi {
    docsPort.set(7973)
}

val contractFile = providers.environmentVariable("SKY_CONTRACT_FILE").map { file(it) }
    .orElse(providers.provider { rootDir.parentFile.parentFile.resolve("docs/api/contract/openapi.yaml") })
    .get()

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(contractFile.toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/contract").get().asFile.absolutePath)
    templateDir.set(rootDir.resolve("openapi-templates"))
    apiPackage.set("com.lukk.sky.message.adapters.inbound.api.contract")
    modelPackage.set("com.lukk.sky.message.adapters.dto.contract")
    globalProperties.set(
        mapOf(
            "apis" to "Messages",
            "models" to "Page,PageableObject,SortObject",
            "supportingFiles" to "false",
        )
    )
    importMappings.set(
        mapOf(
            "ProblemDetail" to "org.springframework.http.ProblemDetail",
            "Pageable" to "org.springframework.data.domain.Pageable",
            "MessageDTO" to "com.lukk.sky.message.adapters.dto.MessageDTO",
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

    implementation(libs.bundles.jaxb)

    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.boot.restclient)
    testImplementation(libs.spring.boot.data.jpa.test)
    testImplementation(libs.spring.boot.jdbc.test)
    testImplementation(libs.testcontainers.postgresql)
}
