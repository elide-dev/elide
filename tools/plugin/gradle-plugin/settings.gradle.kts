pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.14")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.6.0")
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
            credentials {
                username = "apikey"
                password = cachePassword ?: System.getenv("BUILDLESS_APIKEY") ?: error("Failed to resolve cache password")
            }
        }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
