
plugins {
    kotlin("jvm")
}

dependencies {
    val adventureVersion = "4.17.0"
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-gson:$adventureVersion")
}