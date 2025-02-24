@file:Suppress("UnstableApiUsage")

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

pluginManagement {
    repositories {
        maven("https://gradle.pkg.st")
        maven("https://maven.pkg.st")

        maven {
            name = "elide-snapshots"
            url = uri("https://maven.elide.dev")
            content {
                includeGroup("dev.elide")
            }
        }
    }
}

plugins {
    id("build.less")
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.st")
        maven("https://gradle.pkg.st")
        maven {
            name = "elide-snapshots"
            url = uri("https://maven.elide.dev")
            content {
                includeGroup("dev.elide")
                includeGroup("dev.elide.tools")
                includeGroup("org.capnproto")
            }
        }
        maven {
            name = "oss-snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            content {
                includeGroup("dev.elide")
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
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = ("elideGradlePluginBuild")
