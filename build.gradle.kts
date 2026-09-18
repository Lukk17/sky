// Root project: structural only. Per-service builds live under each :sky-* module
// and apply convention plugins from buildSrc.

allprojects {
    group = "com.lukk"
}

tasks.register("printVersions") {
    description = "Print version of every service module."
    group = "help"
    doLast {
        subprojects.forEach { sp ->
            println("${sp.path} -> ${sp.version}")
        }
    }
}
