
rootProject.name = "BlueMap-MarkerManager"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include("bmm-paper")
include("bmm-core")
include("bmm-fabric")
include("kpaper-light")
project(":kpaper-light").projectDir.mkdirs()