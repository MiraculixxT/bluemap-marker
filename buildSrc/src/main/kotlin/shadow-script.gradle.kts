
plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

tasks {
    shadowJar {
        dependencies {
            include {
                it.moduleGroup == "de.miraculixx" || it.moduleGroup == "dev.jorel"
            }
            relocate("dev.jorel.commandapi", "de.miraculixx.bmm.commandapi")
        }
    }
}