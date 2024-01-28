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

import elide.internal.conventions.elide
 import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")

  id("elide.internal.conventions")
}

val buildWasm = project.properties["buildWasm"] == "true"

elide {
  publishing {
    id = "proto-kotlinx"
    name = "Elide Protocol: KotlinX"
    description = "Elide protocol implementation for KotlinX Serialization"
  }

  kotlin {
    target = (KotlinTarget.JVM + KotlinTarget.JsNode).let {
      if(buildWasm) it + KotlinTarget.WASM else it
    }
  }

  jvm {
    forceJvm17 = true
  }

  java {
    configureModularity = false
    includeSources = false
  }
}

dependencies {
  common {
    // API
    api(libs.kotlinx.datetime)
    api(projects.packages.proto.protoCore)
    api(projects.packages.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.protobuf)

    // Implementation
    implementation(kotlin("stdlib"))
    runtimeOnly(kotlin("reflect"))
  }

  commonTest {
    implementation(projects.packages.test)
    implementation(kotlin("test"))
    implementation(projects.packages.proto.protoTest)
  }

  jvm {
    implementation(kotlin("stdlib-jdk8"))
  }

  jvmTest {
    // Testing
    implementation(libs.truth)
    implementation(libs.truth.java8)
  }
}

configurations {
  // `modelInternalJvm` is the dependency used internally by other Elide packages to access the protocol model. at
  // present, the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternalJvm") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["jvmRuntimeClasspath"])
  }
}
