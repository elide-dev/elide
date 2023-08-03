/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
}

plugins {
  id("com.gradle.enterprise") version("3.14.1")
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_PROJECT

  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "elide-benchmarks"

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

include(":graalvm")
include(":server")
