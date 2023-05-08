@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://gradle.pkg.st/")
        maven("https://maven.pkg.st/")
        maven("https://elide.pkg.st/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.5.0")
}

dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.PREFER_PROJECT
    )
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://gradle.pkg.st/")
        maven("https://elide.pkg.st/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
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

val cacheUsername: String? by settings
val cachePassword: String? by settings
val cachePush: String? by settings
val remoteCache = System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: false
val localCache = System.getenv("GRADLE_LOCAL_REMOTE")?.toBoolean() ?: true

buildCache {
    local {
        isEnabled = localCache
    }

    if (remoteCache) {
        remote<HttpBuildCache> {
            isEnabled = true
            isPush = (cachePush ?: System.getenv("GRADLE_CACHE_PUSH")) == "true"
            url = uri("https://gradle.less.build/cache/generic/")
            when (val pswd = cachePassword ?: System.getenv("GRADLE_CACHE_PASSWORD")) {
                null -> {}
                else -> credentials {
                    username = cacheUsername ?: System.getenv("GRADLE_CACHE_USERNAME") ?: "apikey"
                    password = pswd
                }
            }
        }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
