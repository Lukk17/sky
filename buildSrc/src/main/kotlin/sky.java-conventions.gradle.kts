import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
}

val libs = the<LibrariesForLibs>()

repositories {
    mavenCentral()
}

java {
    // Toolchain auto-downloaded by the Foojay convention plugin (declared in root
    // settings.gradle.kts). The catalog's `java` version pin is the single source of
    // truth — bumping it (e.g. 25 -> 26 in a future release) propagates everywhere.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
}

tasks.test {
    useJUnitPlatform()
}
