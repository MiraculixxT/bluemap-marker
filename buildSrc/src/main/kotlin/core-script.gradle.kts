
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.bluecolored.de/releases/")
}

dependencies {
    implementation("org.yaml:snakeyaml:2.5")
    implementation("de.bluecolored:bluemap-api:2.7.6")
    implementation("de.miraculixx:mc-commons:1.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.+")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    // configure Kotlin compile tasks with the new compilerOptions DSL
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
        compilerOptions {
            // set JVM target to 21 to match the Java toolchain/release
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
