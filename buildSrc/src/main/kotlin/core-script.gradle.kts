
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
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.6.2")
}
