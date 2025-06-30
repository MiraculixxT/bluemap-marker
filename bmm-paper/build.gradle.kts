plugins {
    `core-script`
    `paper-script`
    `shadow-script`
}

dependencies {
    implementation(project(":bmm-core"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/data/")
    }
}