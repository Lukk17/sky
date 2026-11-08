// Single source of truth for the JaCoCo tool version.
//
// JaCoCo 0.8.13 supports Java 25 class files. Older versions error on the unknown class
// file major version when the Spring Boot 4 / Java 25 toolchain is used. The convention
// plugin applies it per module and the root applies it for the aggregate tasks.
object JacocoSettings {
    const val TOOL_VERSION = "0.8.13"
}
