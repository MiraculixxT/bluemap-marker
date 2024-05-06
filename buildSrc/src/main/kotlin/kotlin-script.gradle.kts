
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
}