plugins {
    `core-script`
    `fabric-script`
    `adventure-script`
    `publish-script`
}

dependencies {
    implementation(include(project(":bmm-core"))!!)
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/data/")
    }
}
