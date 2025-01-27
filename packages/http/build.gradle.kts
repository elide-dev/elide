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

import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  alias(libs.plugins.elide.conventions)
}

kotlin {
  sourceSets {
    val commonMain by getting
    val commonTest by getting
  }
}

elide {
  publishing {
    id = "http"
    name = "Elide HTTP"
    description = "Cross-platform HTTP utilities and wrappers."
  }

  kotlin {
    target = KotlinTarget.Default
    explicitApi = true
    splitJvmTargets = true
  }

  java {
    configureModularity = false
  }
}

group = "dev.elide"

dependencies {
  common {
    api(projects.packages.base)
    api(projects.packages.core)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.collections.immutable)
  }

  commonTest {
    // Testing
    implementation(projects.packages.test)
    implementation(kotlin("test"))
  }

  jvm {
    implementation(mn.micronaut.http)
  }
}
