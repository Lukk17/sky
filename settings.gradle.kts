rootProject.name = "sky"

include(
    ":sky-common",
    ":sky-booking",
    ":sky-offer",
    ":sky-message",
    ":sky-notify"
)

// The version catalog is auto-discovered at gradle/libs.versions.toml by Gradle.
