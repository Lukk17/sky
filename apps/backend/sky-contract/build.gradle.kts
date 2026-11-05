plugins {
    id("sky.java-library-conventions")
    id("sky.web-conventions")
    id("org.openapi.generator")
}

val contractFile = rootDir.parentFile.parentFile.resolve("docs/api/contract/openapi.yaml")

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(contractFile.toURI().toString())
    outputDir.set(layout.buildDirectory.dir("generated/contract").get().asFile.absolutePath)
    apiPackage.set("com.lukk.sky.contract.api")
    modelPackage.set("com.lukk.sky.contract.dto")
    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "supportingFiles" to "false"
        )
    )
    importMappings.set(
        mapOf(
            "ProblemDetail" to "org.springframework.http.ProblemDetail",
            "Pageable" to "org.springframework.data.domain.Pageable"
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
            "useResponseEntity" to "true",
            "additionalModelTypeAnnotations" to
                "@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)"
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

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("openApiGenerate") {
    inputs.file(contractFile)
}

dependencies {
    implementation(libs.spring.data.commons)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
