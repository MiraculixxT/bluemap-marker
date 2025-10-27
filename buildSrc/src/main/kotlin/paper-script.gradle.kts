import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("de.eldoria.plugin-yml.paper")
    id("com.modrinth.minotaur")
}

description = properties["description"] as String

val gameVersion by properties
val foliaSupport = properties["foliaSupport"] as String == "true"
val projectName = properties["projectName"] as String

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("${properties["gameVersion"]}-R0.1-SNAPSHOT")

    // Kotlin libraries
    library(kotlin("stdlib"))
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")

    // Utility libraries (optional)
    val useBrigadier = properties["useBrigadier"] as String == "true"
    if (useBrigadier) {
        implementation(library("dev.jorel:commandapi-paper-shade:11.0.+")!!)
        implementation(library("dev.jorel:commandapi-kotlin-paper:11.0.+")!!)
    }

    // MC Libraries
    library("de.miraculixx:kpaper-light:1.2.+")
}

//tasks {
//    assemble {
//        dependsOn(reobfJar)
//    }
//}

paper {
    main = "$group.bmm.BMMarker"
    bootstrapper = "$group.bmm.BMMBootstrap"
    loader = "$group.bmm.BMMLoader"
    generateLibrariesJson = true

    name = projectName
    website = "https://mutils.net"

    foliaSupported = true
    apiVersion = "1.20"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP

    serverDependencies {
        register("BlueMap")
    }
}
