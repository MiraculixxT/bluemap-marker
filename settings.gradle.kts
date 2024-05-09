
rootProject.name = "BlueMap-MarkerManager"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

include("bmm-paper")
include("bmm-core")
include("bmm-fabric")
include("kpaper-light")
project(":kpaper-light").projectDir.mkdirs()