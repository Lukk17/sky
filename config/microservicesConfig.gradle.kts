buildscript {
    extra.apply {
        set("springVersion", "6.0.12")
        set("springBootVersion", "3.1.1")
        set("springSecurityTestVersion", "6.1.1")
        set("javaVersion", "VERSION_17")
        set("mysqlVersion", "8.0.33")
        set("gsonVersion", "2.10.1")
        set("h2Version", "2.1.214")
        set("lombokVersion", "1.18.28")
        set("jakartaBindApiVersion", "4.0.0")
        set("jaxbRuntimeVersion", "4.0.3")
        set("kafkaVersion", "3.0.8")
        set("jUnit5Version", "5.10.0-M1")
        set("openapiVersion", "2.1.0")
        set("mockwebserverVersion", "4.11.0")
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
