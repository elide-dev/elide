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
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

rootProject.name = "elide-gradle-plugin"

include(
    ":example:fullstack:browser",
    ":example:fullstack:node",
    ":example:fullstack:server",
    ":example:static:frontend",
    ":example:static:server",
    ":example:mixed",
)
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

buildCache {
    local {
        isEnabled = !(System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: false)
    }
    remote<HttpBuildCache> {
        isEnabled = System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: false
        isPush = (cachePush ?: System.getenv("GRADLE_CACHE_PUSH")) == "true"
        url = uri("https://buildcache.dyme.cloud/gradle/cache/")
        credentials {
            username = cacheUsername ?: System.getenv("GRADLE_CACHE_USERNAME") ?: error("Failed to resolve cache username")
            password = cachePassword ?: System.getenv("GRADLE_CACHE_PASSWORD") ?: error("Failed to resolve cache password")
        }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
