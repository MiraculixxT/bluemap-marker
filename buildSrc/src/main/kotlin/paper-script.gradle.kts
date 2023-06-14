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
    implementation("dev.jorel:commandapi-bukkit-kotlin:9.0.3")
    implementation("dev.jorel:commandapi-bukkit-shade:9.0.3")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}
