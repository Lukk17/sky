import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("sky.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// LibrariesForLibs is the generated accessor for the version catalog; required because
// convention plugins cannot use the `libs` identifier directly the way build.gradle.kts can.
val libs = the<LibrariesForLibs>()

dependencies {
    "implementation"(libs.spring.boot.starter.actuator)

    "developmentOnly"(libs.spring.boot.devtools)

    "compileOnly"(libs.lombok)
    "annotationProcessor"(libs.lombok)
    "compileOnly"(libs.spring.boot.configuration.processor)
    "annotationProcessor"(libs.spring.boot.configuration.processor)

    "testImplementation"(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
    }
    "testImplementation"(libs.h2)
    "testImplementation"(libs.spring.security.test)
    "testImplementation"(libs.junit.jupiter)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}
