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


import elide.internal.conventions.Constants.Elide
import elide.internal.conventions.analysis.skipAnalysis
import elide.internal.conventions.publishing.publish

plugins {
  id("version-catalog")
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "bom"
    name = "Elide BOM"
    description = "Version catalog and BOM for the Elide Framework and Runtime."

    // create a custom publication
    publish("catalog") {
      from(components["versionCatalog"])
    }
  }

  // disable code analysis tools for this project
  skipAnalysis()
}

// Elide modules
val libraries = listOf(
  "base",
  "core",
  "embedded",
  "engine",
  "graalvm",
  "graalvm-java",
  "graalvm-jvm",
  "graalvm-kt",
  "graalvm-llvm",
  "graalvm-py",
  "graalvm-rb",
  "graalvm-ts",
  "graalvm-wasm",
  "builder",
  "http",
  "platform",
  "server",
  "sqlite",
  "ssr",
  "terminal",
  "test",
  "tooling",
  "transport-common",
  "transport-epoll",
  "transport-kqueue",
  "transport-unix",
  "transport-uring",
  "uuid",
)

// Peer modules.
val peers = mapOf(
  "guava" to ("com.google.guava:guava" to libs.versions.guava),
  "protobuf" to ("com.google.protobuf:protobuf-java" to libs.versions.protobuf),
  "grpc" to ("io.grpc:grpc-bom" to libs.versions.grpc.java),
  "micronaut" to ("io.micronaut.platform:micronaut-platform" to libs.versions.micronaut.lib),
  "kotlin.sdk" to ("org.jetbrains.kotlin:kotlin-stdlib" to libs.versions.kotlin.sdk),
)

// Generic version aliases.
val versionAliases = mapOf(
  "graalvm" to libs.versions.graalvm.pin,
  "kotlin.language" to libs.versions.kotlin.language,
)

// Kotlin plugin targets.
val kotlinPlugins: List<String> = emptyList()

catalog {
  versionCatalog {
    // map Elide versions
    version("framework", libs.versions.elide.asProvider().get())

    // map each peer version
    peers.forEach { alias, (_, version) -> version(alias, version.get()) }
    versionAliases.forEach { (alias, version) -> version(alias, version.get()) }

    // define the BOM (this module)
    library("bom", Elide.GROUP, "bom").versionRef("framework")

    // define Elide library aliases
    libraries.forEach { libName ->
      library(libName, Elide.GROUP, "elide-$libName").versionRef("framework")
    }

    // define Elide plugin aliases
    kotlinPlugins.forEach { pluginName ->
      library("plugins.$pluginName", Elide.SUBSTRATE_GROUP, "$pluginName-plugin")
        .versionRef("framework")
    }

    // define peer library aliases
    peers.forEach { alias, (group, _) ->
      library(alias, group.split(":").first(), group.split(":").last()).versionRef(
        alias
      )
    }
  }
}
