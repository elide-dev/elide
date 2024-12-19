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

@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

rootProject.name = "elide-samples"

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
        includeGroupAndSubgroups("org.graalvm")
        includeGroupByRegex("org.graalvm.*")

        listOf(
          "org.graalvm.ruby",
          "org.graalvm.llvm",
          "org.graalvm.python",
          "org.graalvm.js",
          "org.graalvm.polyglot",
          "org.graalvm.tools",
        ).forEach {
          includeGroupAndSubgroups(it)
        }
      }
    }
  }
}

plugins {
  id("com.gradle.enterprise") version("3.16.2")
  id("io.micronaut.platform.catalog") version (extra.properties["micronautCatalogVersion"] as String)
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
    google()
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/elide.versions.toml"))
    }
    create("framework") {
      from("dev.elide:elide-bom:${extra.properties["elideVersion"] as String}")
    }
  }
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

include(
  ":server:helloworld",
  ":server:hellocss",
  ":fullstack:basic:frontend",
  ":fullstack:basic:server",
  ":fullstack:ssr:node",
  ":fullstack:ssr:server",
  ":fullstack:react:frontend",
  ":fullstack:react:server",
  ":fullstack:react-ssr:frontend",
  ":fullstack:react-ssr:node",
  ":fullstack:react-ssr:server",
)
