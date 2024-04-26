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

import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp")

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "processor"
    name = "Elide KSP Processor"
    description = "Annotation processor for KSP and Elide"

    publish("maven") {
      artifactId = "processor"
      groupId = "dev.elide.tools"
      version = rootProject.version as String

      from(components["kotlin"])
    }
  }

  kotlin {
    explicitApi = true
    kotlinVersionOverride = "1.9" // @TODO: ksp processors don't work with 2.0 yet
  }

  java {
    configureModularity = false
  }

  checks {
    ktlint = false
    diktat = false
    spotless = false
  }

  docs {
    enabled = false
  }
}

group = "dev.elide.tools"
version = rootProject.version as String

dependencies {
  // Core platform versions.
  ksp(libs.autoService.ksp)

  // API Deps
  api(libs.jakarta.inject)
  api(libs.slf4j)

  // Protocol dependencies.
  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  implementation(projects.packages.proto.protoKotlinx)

  // Modules
  implementation(projects.packages.base)

  // KSP
  implementation(libs.ksp)
  implementation(libs.ksp.api)
  implementation(libs.google.auto.service)
  implementation(libs.kotlinx.atomicfu)

  // Kotlin
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Protocol Buffers
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)
  implementation(libs.errorprone.annotations)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactive)

  // Testing
  testImplementation(kotlin("test"))
  testImplementation(projects.packages.test)
}
