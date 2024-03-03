
plugins {
    `kotlin-script`
    `paper-script`
    `shadow-script`
}

version = "1.6.3-paper"

dependencies {
    implementation(project(":bmm-core"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/data/")
    }
}