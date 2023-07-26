pluginManagement {
    repositories {
        maven("https://gradle.pkg.st/")
        maven("https://maven.pkg.st/")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://gradle.pkg.st/")
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
