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

plugins {
  kotlin("multiplatform")
  kotlin("plugin.noarg")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "runtime"
    name = "Elide Runtime"
    description = "Runtime context and API definitions."
  }

  kotlin {
    target = KotlinTarget.All
    explicitApi = true
  }

  java {
    configureModularity = false
    includeSources = false
  }
}
