buildscript {
    extra.apply {
        set("springVersion", "2.6.7")
        set("javaVersion", "VERSION_11")
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