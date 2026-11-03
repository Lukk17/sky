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

val repositoryRoot = providers.gradleProperty("sky.openapi.repositoryRoot")
    .map { rootDir.resolve(it) }
    .orElse(providers.provider {
        generateSequence(rootDir.parentFile) { it.parentFile }
            .firstOrNull { it.resolve("docs/api/openapi").isDirectory }
            ?: throw GradleException(
                "Cannot find the repository root above the Gradle root $rootDir: " +
                    "no parent directory contains docs/api/openapi. " +
                    "Set -Psky.openapi.repositoryRoot=<repository root>."
            )
    })

val repositoryOpenApiDir = repositoryRoot.map { root ->
    val docsDir = root.resolve("docs/api/openapi")
    if (docsDir.startsWith(rootDir)) {
        throw GradleException(
            "Refusing to write generated OpenAPI documents inside the Gradle root: $docsDir. " +
                "They belong to the repository outside $rootDir."
        )
    }
    if (!docsDir.isDirectory) {
        throw GradleException(
            "Cannot find the repository OpenAPI directory at $docsDir. " +
                "The Gradle root $rootDir must sit inside the repository, " +
                "or set -Psky.openapi.repositoryRoot=<repository root>."
        )
    }
    docsDir
}

openApi {
    apiDocsUrl.set(docsPort.map { "http://localhost:$it/v3/api-docs/generated.yaml/public" })
    outputDir.fileProvider(repositoryOpenApiDir)
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
