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

import ElidePackages.elidePackage

plugins {
  `version-catalog`
  `maven-publish`
  distribution
  signing
  idea

  id("org.jetbrains.kotlinx.kover")

  id("dev.elide.build.core")
}

group = "dev.elide"
version = rootProject.version as String

// Elide modules.
val libraries = listOf(
  "elide-base",
  "elide-core",
  "elide-test",
  "elide-proto-core",
  "elide-proto-flatbuffers",
  "elide-proto-protobuf",
  "elide-proto-kotlinx",
  "elide-ssr",
  "elide-graalvm",
  "elide-graalvm-js",
  "elide-graalvm-react",
  "elide-model",
  "elide-server",
  "elide-frontend",
  "elide-rpc",
  "elide-ssg",
  "elide-wasm",
)

// Peer modules.
val peers = mapOf(
  "guava" to ("com.google.guava:guava" to libs.versions.guava.get()),
  "protobuf" to ("com.google.protobuf:protobuf-java" to libs.versions.protobuf.get()),
  "grpc" to ("io.grpc:grpc-bom" to libs.versions.grpc.java.get()),
  "micronaut" to ("io.micronaut:micronaut-bom" to libs.versions.micronaut.lib.get()),
)

kover {
  disable()
}

catalog {
  versionCatalog {
    // map Elide versions
    version("elide", libs.versions.elide.asProvider().get())
    version("elidePlugin", libs.versions.elide.plugin.get())

    // map each peer version
    peers.forEach { alias, (_, version) ->
      version(alias, version)
    }

    // define Elide build plugin
    plugin("buildtools", "dev.elide.buildtools.plugin").versionRef("elidePlugin")

    // define the BOM (this module)
    library("elide-bom", Elide.group, "bom").versionRef("elide")

    // define Elide library aliases
    libraries.forEach { libName ->
      library("elide-$libName", Elide.group, libName).versionRef("elide")
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
    tasks.named("publishMavenPublicationToElideRepository"),
    tasks.named("publishMavenPublicationToSonatypeRepository"),
  )
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "dev.elide"
      artifactId = "elide-bom"
      version = rootProject.version as String
      from(components["versionCatalog"])
    }
  }
}

sonarqube {
  isSkipProject = true
}
