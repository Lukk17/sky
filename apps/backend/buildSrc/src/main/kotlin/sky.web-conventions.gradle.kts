import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

dependencies {
    "implementation"(libs.spring.boot.starter.web)
    "implementation"(libs.spring.boot.starter.validation)
    "implementation"(libs.springdoc.openapi.starter.webmvc.ui)
}
