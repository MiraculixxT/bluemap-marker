
plugins {
    kotlin("jvm") version "1.7.20"
    id("io.papermc.paperweight.userdev") version "1.3.8"
    id("xyz.jpenilla.run-paper") version "1.0.6"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.20"
}

group = "de.miraculixx"
version = "1.0.0"

repositories {
    maven {
        name = "JitPack"
        setUrl("https://jitpack.io")
    }
    mavenCentral()
}

dependencies {
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
    implementation("net.axay","kspigot","1.19.0")
    implementation("com.github.BlueMap-Minecraft", "BlueMapAPI", "v2.1.0")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}

tasks {
    runServer {
        minecraftVersion("1.19.2")
    }
}
