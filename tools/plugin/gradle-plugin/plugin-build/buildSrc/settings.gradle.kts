pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
