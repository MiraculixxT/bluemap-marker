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
}

val transitiveInclude: Configuration by configurations.creating {
    exclude(group = "com.mojang")
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.2.1")
    modImplementation("net.silkmc:silk-commands:1.10.2")
    modImplementation("net.silkmc:silk-core:1.10.2")
    modImplementation("net.fabricmc:fabric-loader:0.14.22")
    modImplementation(include("net.kyori:adventure-platform-fabric:5.10.0")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.89.2+1.20.2")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.10.10+kotlin.1.9.10")
    modImplementation(include("me.lucko", "fabric-permissions-api", "0.2-SNAPSHOT"))
    transitiveInclude(implementation("org.yaml:snakeyaml:1.33")!!)

    transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs += "-Xskip-prerelease-check"
        }
    }
}