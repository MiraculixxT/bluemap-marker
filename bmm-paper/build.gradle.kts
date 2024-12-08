plugins {
    `core-script`
    `paper-script`
    `shadow-script`
}

dependencies {
    implementation(project(":bmm-core"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/data/")
    }
}