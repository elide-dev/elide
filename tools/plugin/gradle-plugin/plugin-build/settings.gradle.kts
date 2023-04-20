@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

dependencyResolutionManagement {
    repositoriesMode.set(
        RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )
    repositories {
        mavenCentral()
        gradlePluginPortal()
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
            url = uri("https://buildcache.dyme.cloud/gradle/cache/")
            credentials {
                username = cacheUsername ?: System.getenv("GRADLE_CACHE_USERNAME") ?: error("Failed to resolve cache username")
                password = cachePassword ?: System.getenv("GRADLE_CACHE_PASSWORD") ?: error("Failed to resolve cache password")
            }
        }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
