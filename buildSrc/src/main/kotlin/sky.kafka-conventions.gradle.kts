import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

dependencies {
    "implementation"(libs.spring.boot.starter.kafka)

    "testImplementation"(libs.spring.kafka.test)
}
