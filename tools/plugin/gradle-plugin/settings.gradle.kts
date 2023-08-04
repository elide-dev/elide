/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *     https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

pluginManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://gradle.pkg.st/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.14.1")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.6.0")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.st/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
        maven("https://gradle.pkg.st/")
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
