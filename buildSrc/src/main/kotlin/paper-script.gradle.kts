import BuildConstants.minecraftVersion

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
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.2.1")
    implementation("dev.jorel:commandapi-kotlin:8.8.0")
    implementation("dev.jorel:commandapi-shade:8.8.0")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}
