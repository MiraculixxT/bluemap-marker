import BuildConstants.minecraftVersion
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType

plugins {
    id("fabric-loom")
}

repositories {
    mavenCentral()
    maven {
        name = "JitPack"
        setUrl("https://jitpack.io")
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
}

val transitiveInclude: Configuration by configurations.creating {
    exclude(group = "com.mojang")
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.7.1")
    modImplementation("net.silkmc:silk-commands:1.10.4")
    modImplementation("net.silkmc:silk-core:1.10.4")
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation(include("net.kyori:adventure-platform-fabric:5.12.1-SNAPSHOT")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.97.8+1.20.6")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.10.16+kotlin.1.9.21")
    modImplementation(include("me.lucko", "fabric-permissions-api", "0.3.1"))
    transitiveInclude(implementation("org.yaml:snakeyaml:2.2")!!)

    transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs += "-Xskip-prerelease-check"
        }
    }
}
