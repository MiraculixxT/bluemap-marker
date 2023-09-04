
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven {
        name = "JitPack"
        setUrl("https://jitpack.io")
    }
}

dependencies {
    implementation("org.yaml:snakeyaml:1.33")
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.2.1")
}