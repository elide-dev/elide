/*
 * Copyright (c) 2023-2024 Elide Technologies, Inc.
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

import elide.internal.conventions.kotlin.*

plugins {
  java
  kotlin("multiplatform")
  id("elide.internal.conventions")
}

val buildWasm = project.properties["buildWasm"] == "true"

val commonMain: SourceSet by sourceSets.creating
val commonTest: SourceSet by sourceSets.creating

elide {
  publishing {
    id = "proto-core"
    name = "Elide Protocol: API"
    description = "API headers and services for the Elide Protocol."
  }

  kotlin {
    target = KotlinTarget.All
    explicitApi = true
  }

  java {
    moduleName = "elide.protocol.proto-core"
  }

  jvm {
    forceJvm17 = true
  }
}

dependencies {
  common {
    api(libs.kotlinx.datetime)
    implementation(projects.packages.core)
    implementation(projects.packages.base)
    implementation(kotlin("stdlib"))
  }

  jvm {
    implementation(kotlin("stdlib-jdk8"))
  }

  jvmTest {
    implementation(libs.truth)
    implementation(libs.truth.java8)
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.jupiter.engine)
  }
}

afterEvaluate {
  // @TODO(sgammon): breakage while fetching `joda-core` dependency
  tasks.named("wasmJsBrowserTest").configure {
    enabled = false
  }
}
