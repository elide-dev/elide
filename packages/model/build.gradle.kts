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
import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  kotlin("plugin.noarg")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "model"
    name = "Elide Model"
    description = "Data and structure modeling runtime package for use with the Elide Framework."
  }

  kotlin {
    target = KotlinTarget.All
    explicitApi = true
  }
}

dependencies {
  common {
    api(projects.packages.base)
    api(projects.packages.core)
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.protobuf)
    api(libs.kotlinx.coroutines.core)
  }

  jvm {
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)
    api(libs.flatbuffers.java.core)

    implementation(projects.packages.proto.protoCore)
    implementation(projects.packages.proto.protoProtobuf)
    implementation(projects.packages.proto.protoKotlinx)

    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.kotlinx.serialization.protobuf.jvm)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.jdk9)
    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.jakarta.inject)
    implementation(libs.google.api.common)
    implementation(libs.reactivestreams)
  }

  jvmTest {
    implementation(kotlin("test-junit5"))
    implementation(libs.truth)
    implementation(libs.truth.proto)
    runtimeOnly(libs.junit.jupiter.engine)
    runtimeOnly(libs.logback)
  }

  js {
    // KT-57235: fix for atomicfu-runtime error
    api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.9.20")

    implementation(projects.packages.frontend)
    implementation(libs.kotlinx.coroutines.core.js)
    implementation(libs.kotlinx.serialization.json.js)
    implementation(libs.kotlinx.serialization.protobuf.js)
  }
}
