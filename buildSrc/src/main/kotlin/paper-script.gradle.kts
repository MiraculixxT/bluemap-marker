import BuildConstants.minecraftVersion
import gradle.kotlin.dsl.accessors._4fa44773f276be082611444d94ef06e7.shadowJar

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

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("${minecraftVersion}-R0.1-SNAPSHOT")
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.7.0")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.5.0-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:9.5.0-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(tasks.shadowJar)
    }
}
