buildscript {
    extra.apply {
        set("springBootVersion", "3.5.4")
        set("javaVersion", "VERSION_21")
        set("lombokVersion", "1.18.38")
        set("jUnit5Version", "5.11.0")
        set("openapiVersion", "2.6.0")
        set("mockwebserverVersion", "4.12.0")

    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

apply(plugin = "java")
apply(plugin = "maven-publish")

group = "com.lukk"

//publishing {
//    publications.create<MavenPublication>("maven") {
//        from(components["java"])
//    }
//}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
