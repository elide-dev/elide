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

import elide.internal.conventions.elide
import elide.internal.conventions.analysis.skipAnalysis
import elide.internal.conventions.publishing.publish
import elide.internal.conventions.kotlin.KotlinTarget

plugins {
  id("java-platform")
  id("org.jetbrains.kotlinx.kover")

  id("elide.internal.conventions")
}

elide {
  skipAnalysis()

  publishing {
    id = "platform"
    name = "Elide Platform"
    description = "Elide Platform; Java platform for use with Gradle"

    publish("maven") {
      from(components["javaPlatform"])
    }
  }
}

// Peer modules.
val peers = mapOf(
  "guava" to ("com.google.guava:guava" to libs.versions.guava.get()),
  "protobuf" to ("com.google.protobuf:protobuf-java" to libs.versions.protobuf.get()),
  "grpc" to ("io.grpc:grpc-bom" to libs.versions.grpc.java.get()),
  "netty" to ("io.netty:netty-bom" to libs.versions.netty.asProvider().get()),
  "micronaut" to ("io.micronaut.platform:micronaut-platform" to libs.versions.micronaut.lib.get()),
)

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
