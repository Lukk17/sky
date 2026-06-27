plugins {
    // Auto-downloads JDK toolchains (Java 25) from Adoptium / Azul / etc. via Foojay's
    // Disco API. Needed so the build can target JDK 25 (Spring Boot 4) without every
    // developer having to install JDK 25 by hand.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "sky"

listOf(
    "sky-common",
    "sky-booking",
    "sky-offer",
    "sky-message",
    "sky-notify",
    "sky-gateway"
).filter { rootDir.resolve(it).isDirectory }
    .forEach { include(":$it") }

// The version catalog is auto-discovered at gradle/libs.versions.toml by Gradle.
