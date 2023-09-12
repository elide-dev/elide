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
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "runtime-rb"
    name = "Elide Ruby Runtime"
    description = "Ruby plugin for the Elide runtime."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }
}

dependencies {
  // Modules
  implementation(projects.packages.runtimeCore)
  
  // Kotlin
  implementation(kotlin("stdlib"))
  
  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
