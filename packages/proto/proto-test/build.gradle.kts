/*
 * Copyright (c) 2023-2024 Elide Ventures, LLC.
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
import elide.internal.conventions.publishing.publish
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  id("elide.internal.conventions")
}

val buildWasm = project.properties["buildWasm"] == "true"

val commonMain: SourceSet by sourceSets.creating

elide {
  publishing {
    id = "proto-test"
    name = "Elide Protocol: Testing"
    description = "Multiplatform testing utilities for protocol implementations."
  }

  kotlin {
    target = KotlinTarget.All
  }

  jvm {
    forceJvm17 = true
  }

  // disable module-info processing (not present)
  java {
    configureModularity = false
  }
}

dependencies {
  common {
    api(kotlin("stdlib"))
    api(kotlin("test"))
    api(libs.kotlinx.datetime)
    api(projects.packages.core)
    api(projects.packages.base)
    api(projects.packages.test)
    api(projects.packages.proto.protoCore)
  }
}
