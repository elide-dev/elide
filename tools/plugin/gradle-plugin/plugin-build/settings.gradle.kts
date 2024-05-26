/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://gradle.pkg.st")
        maven("https://maven.pkg.st")

        maven {
            name = "elide-snapshots"
            url = uri("https://maven.elide.dev")
            content {
                includeGroup("dev.elide")
                includeGroup("com.google.devtools.ksp")
                includeGroup("org.jetbrains.reflekt")
            }
        }
    }
}

plugins {
    id("build.less") version("1.0.0-rc2")
    id("com.gradle.develocity") version("3.17.4")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        maven("https://maven.pkg.st")
        maven("https://gradle.pkg.st")

        maven {
            name = "elide-snapshots"
            url = uri("https://maven.elide.dev")
            content {
                includeGroup("dev.elide")
                includeGroup("dev.elide.tools")
                includeGroup("com.google.devtools.ksp")
                includeGroup("org.jetbrains.reflekt")
            }
        }
        maven {
            name = "wasm-dev"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
            content {
                includeGroup("io.ktor")
                includeGroup("org.jetbrains.compose")
                includeGroup("org.jetbrains.compose.compiler")
                includeGroup("org.jetbrains.kotlin")
                includeGroup("org.jetbrains.kotlinx")
            }
        }
        maven {
            name = "compose-dev"
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
            content {
                includeGroup("androidx.compose")
                includeGroup("androidx.compose.compiler")
                includeGroup("org.jetbrains.compose")
                includeGroup("org.jetbrains.compose.compiler")
                includeGroup("web")
            }
        }
        maven {
            name = "compose-edge"
            url = uri("https://androidx.dev/storage/compose-compiler/repository/")
            content {
                includeGroup("androidx.compose")
                includeGroup("androidx.compose.compiler")
                includeGroup("org.jetbrains.compose")
                includeGroup("org.jetbrains.compose.compiler")
            }
        }
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = ("elideGradlePlugin")

include(
    ":plugin"
)

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
