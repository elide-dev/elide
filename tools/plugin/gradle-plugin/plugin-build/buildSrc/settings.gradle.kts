pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        google()
        maven("https://plugins.gradle.org/m2/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
