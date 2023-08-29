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
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import ElidePackages.elidePackage

plugins {
  id("com.google.devtools.ksp")

  id("dev.elide.build.jvm")
  id("dev.elide.build.publishable")
}

group = "dev.elide.tools"
version = rootProject.version as String

kotlin {
  explicitApi()

  publishing {
    publications {
      create<MavenPublication>("maven") {
        artifactId = "processor"
        groupId = "dev.elide.tools"
        version = rootProject.version as String
      }
    }
  }
}

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

elidePackage(
  id = "processor",
  name = "Elide KSP Processor",
  description = "Annotation processor for KSP and Elide",
) {
  java9Modularity = false
}
