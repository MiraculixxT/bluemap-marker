
plugins {
    `kotlin-script`
    `fabric-script`
    `adventure-script`
    `shadow-script`
}

dependencies {
    implementation(project(":bmm-core"))
    include(project(":bmm-core"))
    modImplementation("net.silkmc:silk-commands:1.9.2")
    modImplementation(include("net.kyori:adventure-platform-fabric:5.5.0")!!)
}