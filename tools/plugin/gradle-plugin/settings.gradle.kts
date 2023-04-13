pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.11.4")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
        gradlePluginPortal()
    }
}

rootProject.name = "elide-gradle-plugin"

val buildExamples: String? by settings
if (buildExamples == "true") {
    include(
        ":example:fullstack:browser",
        ":example:fullstack:node",
        ":example:fullstack:server",
        ":example:static:frontend",
        ":example:static:server",
        ":example:mixed",
    )
}
includeBuild("plugin-build")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

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
            isUseExpectContinue = true
            url = uri(System.getenv("CACHE_ENDPOINT") ?: "https://global.less.build/cache/generic/")
            credentials {
                username = cacheUsername ?: System.getenv("GRADLE_CACHE_USERNAME") ?: error("Failed to resolve cache username")
                password = cachePassword ?: System.getenv("GRADLE_CACHE_PASSWORD") ?: error("Failed to resolve cache password")
            }
        }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
