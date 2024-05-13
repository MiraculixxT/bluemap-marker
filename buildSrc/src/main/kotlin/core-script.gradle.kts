
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.bluecolored.de/releases")
}

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
    implementation("de.bluecolored.bluemap:BlueMapAPI:2.7.1")
    implementation("de.miraculixx:mc-commons:1.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}
