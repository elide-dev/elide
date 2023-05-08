pluginManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://gradle.pkg.st/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://gradle.pkg.st/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
