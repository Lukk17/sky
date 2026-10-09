import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("sky.java-conventions")
    id("io.spring.dependency-management")
}

val libs = the<LibrariesForLibs>()

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    }
}

dependencies {
    "compileOnly"(libs.lombok)
    "annotationProcessor"(libs.lombok)

    "testImplementation"(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
    }
    "testImplementation"(libs.junit.jupiter)
}
