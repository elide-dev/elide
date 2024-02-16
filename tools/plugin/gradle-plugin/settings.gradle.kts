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
        maven("https://maven.pkg.st")
        maven("https://gradle.pkg.st")
        maven {
            name = "elide-snapshots"
            url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
            content {
                includeGroup("dev.elide")
                includeGroup("org.capnproto")
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
    }
}

plugins {
    id("build.less") version("1.0.0-rc2")
    id("com.gradle.enterprise") version("3.16.2")
    id("com.gradle.common-custom-user-data-gradle-plugin") version("1.12.1")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.st")
        maven("https://gradle.pkg.st")

        maven {
            name = "elide-snapshots"
            url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
            content {
                includeGroup("dev.elide")
                includeGroup("org.capnproto")
            }
        }
        maven {
            name = "oss-snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            content {
                includeGroup("dev.elide")
                includeGroup("com.google.devtools.ksp")
                includeGroup("org.jetbrains.reflekt")
            }
        }
        maven {
            name = "dokka-dev"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
            content {
                includeGroup("org.jetbrains.dokka")
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
        mavenLocal()
    }
}

rootProject.name = "elide-gradle-plugin"
includeBuild("plugin-build")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
