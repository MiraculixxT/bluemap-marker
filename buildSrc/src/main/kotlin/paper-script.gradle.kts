import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit")
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

//paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

dependencies {
    paperweight.paperDevBundle("${properties["gameVersion"]}-R0.1-SNAPSHOT")

    // Kotlin libraries
    library(kotlin("stdlib"))
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")

    // Utility libraries (optional)
    val useBrigadier = properties["useBrigadier"] as String == "true"
    if (useBrigadier) {
        implementation("dev.jorel:commandapi-bukkit-shade:9.5.0")
        implementation("dev.jorel:commandapi-bukkit-kotlin:9.5.0")
    }

    // MC Libraries
    implementation("de.miraculixx:kpaper-light:1.2.1")
}

tasks {
    assemble {
        dependsOn(tasks.shadowJar)
    }
}

bukkit {
    main = "$group.bmm.BMMarker"
    apiVersion = "1.16"
    foliaSupported = foliaSupport
    name = projectName
    website = "https://mutils.net"
    version = properties["version"] as String
    description = properties["description"] as String

    // Optionals
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    depend = listOf("BlueMap")
    softDepend = listOf()
}
