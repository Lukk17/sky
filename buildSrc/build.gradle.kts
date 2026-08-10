plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${libs.versions.spring.boot.get()}")
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:${libs.versions.spring.dependency.management.get()}")

    // Make the version-catalog `libs` accessor available inside convention plugins
    // (precompiled .gradle.kts under src/main/kotlin/). Gradle generates the
    // LibrariesForLibs class onto buildSrc's compileClasspath through this trick.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
