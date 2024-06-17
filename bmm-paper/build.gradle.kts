plugins {
    `core-script`
    `paper-script`
    `shadow-script`
}

version = "1.6.4-paper"

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