pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "L-Language-Dev"
include("lutil")
include("lvm")
include("lvm-bytecode-generator")
include("lg")
include("lc")
include("llvm-ir-generator")
include("litec")
include("lpm")