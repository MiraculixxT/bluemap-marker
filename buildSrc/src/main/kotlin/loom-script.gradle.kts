
plugins {
    id("fabric-loom")
    id("io.github.juuxel.loom-quiltflower")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.2")
    mappings(loom.layered {
        officialMojangMappings()
    })
}