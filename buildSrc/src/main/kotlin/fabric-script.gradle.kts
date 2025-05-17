plugins {
    id("fabric-loom")
    id("io.github.dexman545.outlet")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val transitiveInclude: Configuration by configurations.creating {
    exclude(group = "com.mojang")
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
}

dependencies {
    val gameVersion: String by properties
    outlet.mcVersionRange = properties["fabricDependencyVersions"] as String

    //
    // Fabric configuration
    //
    minecraft("com.mojang", "minecraft", gameVersion)
    mappings(loom.officialMojangMappings())
//    println("FabricLoader: " + outlet.loaderVersion() + ", " + outlet.fapiVersion())
//    modImplementation("net.fabricmc", "fabric-loader", outlet.loaderVersion())
//    modImplementation("net.fabricmc.fabric-api", "fabric-api", outlet.fapiVersion())
    modImplementation("net.fabricmc", "fabric-loader", "0.16.12")
    modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.119.9+1.21.5")

    //
    // Kotlin libraries
    //
    val flkVersion = outlet.latestModrinthModVersion("fabric-language-kotlin", outlet.mcVersions())
    modImplementation("net.fabricmc", "fabric-language-kotlin", flkVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.+")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.+")
    modImplementation(include("net.kyori:adventure-platform-fabric:${properties["adventureVersion"]}")!!)

    //
    // Silk configuration (optional)
    //
    val silkVersion = properties["silkVersion"] as String
    println("Silk: $silkVersion")
    modImplementation("net.silkmc", "silk-core", silkVersion)
    modImplementation("net.silkmc", "silk-commands", silkVersion) // easy command registration
    modImplementation("net.silkmc", "silk-nbt", silkVersion) // item simplification
    modImplementation("net.silkmc", "silk-network", silkVersion)


    //
    // Permissions configuration (optional)
    //
    val usePermissions = properties["usePermissions"] as String == "true"
    if (usePermissions) {
        modImplementation(include("me.lucko", "fabric-permissions-api", "0.3.3"))
    }

    //
    // Configuration
    //
    transitiveInclude(implementation("org.yaml", "snakeyaml", "2.2"))

    // Add all non-mod dependencies to the jar
    include("de.miraculixx:mc-commons:1.0.1")
    transitiveInclude.resolvedConfiguration.resolvedArtifacts.forEach {
        include(it.moduleVersion.id.toString())
    }
}

tasks.processResources {
    println("-----" + outlet.mcVersionRange + " - ${properties["version"]}")
    filesMatching("fabric.mod.json") {
        val modrinthSlug = properties["modrinthProjectId"] as? String ?: properties["modid"] as String
        expand(
            mapOf(
                "modid" to properties["modid"] as String,
                "version" to properties["version"] as String,
                "name" to properties["projectName"] as String,
                "description" to properties["description"],
                "author" to properties["author"] as String,
                "license" to properties["licence"] as String,
                "modrinth" to modrinthSlug,
                "environment" to properties["environment"] as String,
                "mcversion" to outlet.mcVersionRange,
            )
        )
    }
}
