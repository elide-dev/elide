pluginManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://gradle.pkg.st/")
        maven("https://elide.pkg.st/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.13.1")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.5.0")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://elide.pkg.st/")
        maven("https://gradle.pkg.st/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
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
            url = uri(System.getenv("CACHE_ENDPOINT") ?: "https://gradle.less.build/cache/generic/")
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
