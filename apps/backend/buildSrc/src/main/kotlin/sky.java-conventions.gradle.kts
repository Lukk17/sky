import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
    id("sky.jacoco-conventions")
}

val libs = the<LibrariesForLibs>()

repositories {
    mavenCentral()
}

java {
    // Toolchain auto-downloaded by the Foojay convention plugin (declared in root
    // settings.gradle.kts). The catalog's `java` version pin is the single source of
    // truth. Bumping it (e.g. 25 -> 26 in a future release) propagates everywhere.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
}

dependencies {
    constraints {
        // Force the patched commons-lang3 over the version springdoc-openapi pulls
        // transitively (3.17.0 carries CVE-2025-48924, uncontrolled recursion / DoS).
        "implementation"("org.apache.commons:commons-lang3:3.18.0")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1536m"
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

// Removes orphaned Testcontainers left behind on red builds or Ctrl+C.
// Containers carry label sky-testcontainer=true, so only test containers match.
val pruneSkyTestcontainers by tasks.registering(Exec::class) {
    group = "verification"
    description = "Prune orphaned sky Testcontainers by label."
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine(
            "powershell", "-NoProfile", "-NonInteractive", "-Command",
            "\$ids = docker ps -aq --filter 'label=sky-testcontainer=true'; " +
                "if (\$ids) { docker rm -f \$ids } else { exit 0 }"
        )
    } else {
        commandLine(
            "sh", "-c",
            "ids=\$(docker ps -aq --filter 'label=sky-testcontainer=true'); " +
                "if [ -n \"\$ids\" ]; then docker rm -f \$ids; fi; exit 0"
        )
    }
    isIgnoreExitValue = true
}

tasks.named<Test>("test") {
    finalizedBy(pruneSkyTestcontainers)
}
