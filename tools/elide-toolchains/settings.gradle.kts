/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "elide-toolchains-plugin"

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
  id("com.gradle.develocity") version("3.17.4")
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
        includeGroup("org.capnproto")
        includeGroup("com.google.devtools.ksp")
        includeGroup("org.jetbrains.reflekt")
      }
    }
    google()
  }
  
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
  }
}
