
plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

tasks {
    shadowJar {
        dependencies {
            include {
                it.moduleGroup == properties["group"] as String || it.moduleGroup == "dev.jorel"
            }
        }
        relocate("dev.jorel", "de.miraculixx.bmm")
    }

    assemble {
        dependsOn(shadowJar)
    }
}