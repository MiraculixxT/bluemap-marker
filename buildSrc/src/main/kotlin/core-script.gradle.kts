
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
//    val ktorVersion = "2.1.3"
//    implementation("io.ktor:ktor-client-core:$ktorVersion")
//    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.yaml:snakeyaml:1.33")
    implementation("com.github.BlueMap-Minecraft:BlueMapAPI:v2.2.1")
}