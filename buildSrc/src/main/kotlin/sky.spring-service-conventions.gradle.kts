import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("sky.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    jacoco
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
    "testImplementation"(libs.archunit.junit5)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

// JaCoCo coverage report (visibility), with a verification gate available but
// NOT wired into `check` yet.
//
// Why no gate today: sky-notify sits at ~6% branch coverage (audit found 2 tests
// for 15+ production classes). Wiring the gate now would block every PR until
// the test-modernization backfill (Testcontainers integration tests, @DataJpaTest
// slices, @WithMockUser security tests, full sky-notify backfill) lands and
// raises the floor. The verification task exists so future contributors can
// invoke `./gradlew jacocoTestCoverageVerification` and the gate flips into
// `check` once coverage clears the modest 0.30 line / 0.20 branch threshold.
// Target after full backfill: 0.80 line / 0.70 branch per the change's spec.
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",
                    "**/config/**",
                    "**/*Application.class",
                    "**/Constants.class",
                    "**/architecture/**"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.30".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.20".toBigDecimal()
            }
        }
    }
}
