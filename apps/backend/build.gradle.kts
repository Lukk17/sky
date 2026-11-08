import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("base")
    jacoco
    alias(libs.plugins.org.owasp.dependencycheck)
}

allprojects {
    group = "com.lukk"
}

// The root itself now owns JaCoCo and OWASP tasks, so it needs a repository to resolve
// their tool dependencies. Module dependencies stay declared in the convention plugins.
repositories {
    mavenCentral()
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

// OWASP Dependency Check at the root so CI has one entry point, the aggregate task,
// rather than one scan per module. A High or Critical finding fails the scan.
dependencyCheck {
    failBuildOnCVSS = 7.0f
}

// The aggregate tasks below are JaCoCo tasks, so the root needs the jacoco plugin to
// supply their tool classpath. The version matches the per-module convention plugin.
jacoco {
    toolVersion = JacocoSettings.TOOL_VERSION
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

// Aggregate JaCoCo report and verification gate.
//
// The per-module gate in sky.jacoco-conventions is authoritative. The aggregate is
// consistent with it by construction: it reads each gated module's own
// build/jacoco/*.exec, measures only that module's main classes, and applies the same
// CoverageFilters.exclusions the per-module report and gate use. A module that opts out
// by disabling jacocoTestCoverageVerification (sky-gateway) is left out of the aggregate
// too, so the aggregate number never dilutes or hides a module that is not gated.
fun aggregateExecutionData(modules: List<Project>) =
    files(modules.map { module ->
        fileTree(module.layout.buildDirectory.dir("jacoco")) {
            include("*.exec")
        }
    })

fun aggregateClassDirectories(modules: List<Project>) =
    files(modules.map { module ->
        fileTree(module.layout.buildDirectory.dir("classes/java/main")) {
            exclude(CoverageFilters.exclusions)
        }
    })

tasks.register<JacocoReport>("jacocoAggregateReport") {
    group = "verification"
    description = "Generates an aggregate JaCoCo report from every gated subproject."
}

tasks.register<JacocoCoverageVerification>("jacocoAggregateVerification") {
    group = "verification"
    description = "Verifies aggregate code coverage meets the per-module thresholds."

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

// Subproject plugins and their jacoco task state are only final after every project has
// been evaluated, so the aggregate inputs are wired here rather than in the register block.
gradle.projectsEvaluated {
    val gatedModules = subprojects.filter { module ->
        module.plugins.hasPlugin("jacoco") &&
            module.tasks.named("jacocoTestCoverageVerification").get().enabled
    }

    tasks.named<JacocoReport>("jacocoAggregateReport") {
        // Running the tests is what creates each module's exec file.
        dependsOn(gatedModules.map { it.tasks.named("test") })
        executionData.setFrom(aggregateExecutionData(gatedModules))
        classDirectories.setFrom(aggregateClassDirectories(gatedModules))
        sourceDirectories.setFrom(files(gatedModules.map { it.file("src/main/java") }))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoAggregateVerification") {
        dependsOn(gatedModules.map { it.tasks.named("test") })
        executionData.setFrom(aggregateExecutionData(gatedModules))
        classDirectories.setFrom(aggregateClassDirectories(gatedModules))
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoAggregateVerification"))
}
