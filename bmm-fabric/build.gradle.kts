import dex.plugins.outlet.v2.util.ReleaseType

plugins {
    `core-script`
    `fabric-script`
    `adventure-script`
    `publish-script`
    id("io.github.dexman545.outlet")
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(project(":bmm-core"))
    include(project(":bmm-core"))
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/data/")
    }
}

modrinth {
    this.versionName = "BMM Fabric - ${(version as String).removeSuffix("-fabric")}"
    uploadFile.set(tasks.remapJar)
    outlet.mcVersionRange = properties["fabricSupportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("fabric")
        add("quilt")
    })
    dependencies {
        // The scope can be `required`, `optional`, `incompatible`, or `embedded`
        // The type can either be `project` or `version`
//        required.project("fabric-api")
        required.project("fabric-language-kotlin")

        required.project("silk")
        required.project("bluemap")
    }
}