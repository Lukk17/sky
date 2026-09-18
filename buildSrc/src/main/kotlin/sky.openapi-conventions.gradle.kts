plugins {
    java
    id("org.springframework.boot")
    id("org.springdoc.openapi-gradle-plugin")
}

interface SkyOpenApiExtension {
    val docsPort: Property<Int>
}

val skyOpenApi = extensions.create<SkyOpenApiExtension>("skyOpenApi")

val docsPort = skyOpenApi.docsPort

openApi {
    apiDocsUrl.set(docsPort.map { "http://localhost:$it/v3/api-docs/generated.yaml/public" })
    outputDir.set(file("$rootDir/docs/api/openapi"))
    outputFileName.set("${project.name}.openapi.yaml")
    waitTimeInSeconds.set(180)
    customBootRun {
        args.set(docsPort.map {
            listOf("--spring.profiles.active=local,openapi", "--server.port=$it")
        })
    }
}

tasks.matching { it.name in setOf("forkedSpringBootRun", "forkedSpringBootStop", "generateOpenApiDocs") }
    .configureEach {
        notCompatibleWithConfigurationCache("springdoc-openapi-gradle-plugin uses JavaExecFork")
    }

tasks.named("forkedSpringBootRun") {
    dependsOn(":sky-common:jar")
}

tasks.named("build") {
    dependsOn("generateOpenApiDocs")
}
