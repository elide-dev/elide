@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = ("dev.elide.buildtools.gradle")

include(
    ":plugin"
)
