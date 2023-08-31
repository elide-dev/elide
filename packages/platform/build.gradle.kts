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

plugins {
  `maven-publish`
  `java-platform`
  distribution
  signing
  idea

  id("org.jetbrains.kotlinx.kover")

  id("dev.elide.build.core")
  id("dev.elide.build.publishable")
  id("dev.sigstore.sign")
}

group = "dev.elide"
version = rootProject.version as String

// Peer modules.
val peers = mapOf(
  "guava" to ("com.google.guava:guava" to Versions.guava),
  "protobuf" to ("com.google.protobuf:protobuf-java" to Versions.protobuf),
  "grpc" to ("io.grpc:grpc-bom" to Versions.grpc),
  "netty" to ("io.netty:netty-bom" to Versions.netty),
  "micronaut" to ("io.micronaut.platform:micronaut-platform" to Versions.micronaut),
)

kover {
  disable()
}

dependencies {
  constraints {
    // BOMs: gRPC, Netty, Micronaut.
    api(libs.grpc.bom)
    api(libs.netty.bom)
    api(libs.projectreactor.bom)

    // Kotlin.
    api(kotlin("stdlib"))

    // Google: Protocol Buffers, Guava, GAX, gRPC.
    api(libs.guava)
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)

    // KotlinX: Co-routines.
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.core.jvm)
    api(libs.kotlinx.collections.immutable)

    // KotlinX: Datetime.
    api(libs.kotlinx.datetime)

    // KotlinX: Serialization.
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.core.jvm)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.json.jvm)
    api(libs.kotlinx.serialization.protobuf)
    api(libs.kotlinx.serialization.protobuf.jvm)
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["javaPlatform"])
      groupId = "dev.elide"
      artifactId = "elide-platform"
      version = project.version as String

      pom {
        name = "Elide Platform"
        url = "https://elide.dev"
        description = "Elide Platform and catalog for Gradle."

        licenses {
          license {
            name = "MIT License"
            url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
          }
        }
        developers {
          developer {
            id = "sgammon"
            name = "Sam Gammon"
            email = "samuel.gammon@gmail.com"
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

val publishAllElidePublications by tasks.registering {
  group = "Publishing"
  description = "Publish all release publications for this Elide package"
  dependsOn(
    tasks.named("publishMavenPublicationToElideRepository"),
    tasks.named("publishMavenPublicationToSonatypeRepository"),
  )
}
