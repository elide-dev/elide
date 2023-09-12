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
  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "frontend"
    name = "Elide Frontend"
    description = "Tools for building UI experiences on top of the Elide Framework/Runtime."
  }

  kotlin {
    target = KotlinTarget.JsBrowser
    explicitApi = true
  }
}

dependencies {
  js {
    implementation(projects.packages.base)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
  }

  jsTest {
    implementation(projects.packages.test)
  }
}
