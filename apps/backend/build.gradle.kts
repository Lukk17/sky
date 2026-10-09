plugins {
    id("base")
}

import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

allprojects {
    group = "com.lukk"
}

subprojects {
    // versionFromChangelog: parse the top ## [x.y.z] from each module CHANGELOG.md
    afterEvaluate {
        val changelog = file("CHANGELOG.md")
        if (changelog.exists()) {
            val content = changelog.readText()
            val pattern = """##\s+\[(\d+\.\d+\.\d+)\]""".toRegex()
            val match = pattern.find(content)
            if (match != null) {
                version = match.groupValues[1]
            }
        }
    }
}

tasks.register("printVersions") {
    description = "Print version of every service module."
    group = "help"
    doLast {
        subprojects.forEach { sp ->
            println("  ${sp.name}: ${sp.version}")
        }
    }
}

// Aggregate JaCoCo report and verification gate
tasks.register<JacocoReport>("jacocoAggregateReport") {
    group = "verification"
    description = "Generates an aggregate JaCoCo report from all subprojects."
    dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })

    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/**/*.exec")
    })
    classDirectories.setFrom(fileTree(project.projectDir) {
        subprojects.forEach { sp ->
            include("${sp.name}/build/classes/**")
        }
    })
    sourceDirectories.setFrom(fileTree(project.projectDir) {
        subprojects.forEach { sp ->
            include("${sp.name}/src/main/**")
        }
    })

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoAggregateVerification") {
    group = "verification"
    description = "Verifies aggregate code coverage meets thresholds."
    dependsOn(tasks.named("jacocoAggregateReport"))

    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/**/*.exec")
    })
    classDirectories.setFrom(fileTree(project.projectDir) {
        subprojects.forEach { sp ->
            if (sp.tasks.findByName("jacocoTestCoverageVerification")?.enabled != false) {
                include("${sp.name}/build/classes/**")
            }
        }
    })

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

tasks.named("check") {
    dependsOn(tasks.named("jacocoAggregateVerification"))
}
