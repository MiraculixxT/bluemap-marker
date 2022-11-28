import BuildConstants.minecraftVersion
import gradle.kotlin.dsl.accessors._5b841d749b44c4d634f7cfea3ed45134.implementation

plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
}

repositories {
    mavenCentral()
    maven {
        name = "JitPack"
        setUrl("https://jitpack.io")
    }
}

dependencies {
    paperDevBundle("${minecraftVersion}-R0.1-SNAPSHOT")
    implementation("net.axay:kspigot:1.19.0")
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.2.1")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}
