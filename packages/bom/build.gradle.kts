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

@file:Suppress("UnstableApiUsage")

import ElidePackages.allDevelopers

plugins {
  `version-catalog`
  `maven-publish`
  distribution
  signing
  idea

  id("org.jetbrains.kotlinx.kover")

  id("dev.elide.build.core")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

// Elide modules.
val libraries = listOf(
  "base",
  "core",
  "test",
  "proto-core",
  "proto-flatbuffers",
  "proto-protobuf",
  "proto-kotlinx",
  "proto-capnp",
  "ssr",
  "graalvm",
  "graalvm-js",
  "graalvm-jvm",
  "graalvm-kt",
  "graalvm-llvm",
  "graalvm-py",
  "graalvm-rb",
  "graalvm-react",
  "graalvm-wasm",
  "model",
  "platform",
  "server",
  "frontend",
  "rpc",
  "ssg",
  "uuid",
  "wasm",
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
  "graalvm" to libs.versions.graalvm.sdk,
  "kotlin.language" to libs.versions.kotlin.language,
)

// Kotlin plugin targets.
val kotlinPlugins = listOf(
  "redakt",
)

kover {
  disable()
}

catalog {
  versionCatalog {
    // map Elide versions
    version("elide.framework", libs.versions.elide.asProvider().get())
    version("elide.plugin", libs.versions.elide.plugin.get())

    // map each peer version
    peers.forEach { alias, (_, version) ->
      version(alias, version.get())
    }
    versionAliases.forEach { (alias, version) ->
      version(alias, version.get())
    }

    // define Elide build plugin
    plugin("buildtools", "dev.elide.buildtools.plugin").versionRef("elide.plugin")

    // define the BOM (this module)
    library("elide.bom", Elide.group, "bom").versionRef("elide.framework")

    // define Elide library aliases
    libraries.forEach { libName ->
      library("elide.$libName", Elide.group, "elide-$libName").versionRef("elide.framework")
    }

    // define Elide plugin aliases
    kotlinPlugins.forEach { pluginName ->
      library("elide.plugins.$pluginName", Elide.substrateGroup, "$pluginName-plugin")
        .versionRef("elide.framework")
    }

    // define peer library aliases
    peers.forEach { alias, (group, _) ->
      library(alias, group.split(":").first(), group.split(":").last()).versionRef(
        alias
      )
    }
  }
}

val publishAllElidePublications by tasks.registering {
  group = "Publishing"
  description = "Publish all release publications for this Elide package"
  dependsOn(
    tasks.named("publishAllPublicationsToElideRepository"),
    tasks.named("publishAllPublicationsToSonatypeRepository"),
  )
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "dev.elide"
      artifactId = "elide-bom"
      version = rootProject.version as String

      from(components["versionCatalog"])

      pom {
        name = "Elide BOM"
        url = "https://elide.dev"
        description = "Version catalog and BOM for the Elide Framework and Runtime"

        licenses {
          license {
            name = "MIT License"
            url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
          }
        }
        allDevelopers.map {
          developers {
            developer {
              id = it.id
              name = it.name
              if (it.email != null) {
                email = it.email
              }
            }
          }
        }
        scm {
          url = "https://github.com/elide-dev/elide"
        }
      }
    }
  }
}

sonarqube {
  isSkipProject = true
}
