plugins {
    java
    jacoco
}

// JaCoCo coverage: one filtered class set, reported for visibility and gated in `check`.
val coverageExcludes = listOf(
    "**/dto/**",
    "**/config/**",
    "**/*Application.class",
    "**/Constants.class"
)

fun measuredClasses(classDirs: FileCollection): FileCollection =
    files(classDirs.files.map { dir -> fileTree(dir) { exclude(coverageExcludes) } })

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(measuredClasses(classDirectories))
}

// JaCoCo 0.8.13 supports Java 25 class files. Older versions error on unknown class
// file major version when the Spring Boot 4 / Java 25 toolchain bump lands.
jacoco {
    toolVersion = "0.8.13"
}

// A module whose measured set is empty opts itself out in its own build file, by
// disabling jacocoTestCoverageVerification there. This plugin names no module.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(measuredClasses(classDirectories))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
