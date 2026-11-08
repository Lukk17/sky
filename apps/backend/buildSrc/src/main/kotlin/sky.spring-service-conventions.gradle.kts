import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("sky.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// LibrariesForLibs is the generated accessor for the version catalog; required because
// convention plugins cannot use the `libs` identifier directly the way build.gradle.kts can.
val libs = the<LibrariesForLibs>()

// Force the Testcontainers version from the catalog. Spring Boot 4 BOM manages 2.x by default,
// but the explicit force here ensures the catalog pin (2.0.5) is authoritative regardless of
// what any future BOM bump would pull in.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.testcontainers") {
            useVersion(libs.versions.testcontainers.get())
            because("Testcontainers 2.x required for Docker Engine 29+ compatibility")
        }
    }
}

dependencies {
    "implementation"(libs.spring.boot.starter.actuator)
    "implementation"(libs.micrometer.registry.prometheus)

    "developmentOnly"(libs.spring.boot.devtools)

    "compileOnly"(libs.lombok)
    "annotationProcessor"(libs.lombok)
    "compileOnly"(libs.spring.boot.configuration.processor)
    "annotationProcessor"(libs.spring.boot.configuration.processor)

    "testImplementation"(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
    }
    "testImplementation"(libs.spring.security.test)
    "testImplementation"(libs.junit.jupiter)
    "testImplementation"(libs.archunit.junit5)

    // Testcontainers: pinned via BOM (test-only), per-service base classes pick the
    // modules they need (mysql / kafka). spring-boot-testcontainers provides
    // @ServiceConnection so subclasses don't need @DynamicPropertySource.
    "testImplementation"(platform(libs.testcontainers.bom))
    "testImplementation"(libs.testcontainers.junit.jupiter)
    "testImplementation"(libs.spring.boot.testcontainers)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}
