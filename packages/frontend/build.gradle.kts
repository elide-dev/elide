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
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import ElidePackages.elidePackage

plugins {
  id("dev.elide.build.js")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val buildWasm = project.properties["buildWasm"] == "true"

kotlin {
  explicitApi()

  js {
    browser()
    generateTypeScriptDefinitions()

    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
  }
  if (buildWasm) wasm {
    d8()
  }

  sourceSets {
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(projects.packages.base)

        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        implementation(libs.kotlinx.serialization.protobuf.js)
      }
    }

    val jsTest by getting {
      dependencies {
        implementation(projects.packages.test)
      }
    }
  }
}

elidePackage(
  id = "frontend",
  name = "Elide Frontend",
  description = "Tools for building UI experiences on top of the Elide Framework/Runtime.",
) {
  java9Modularity = false
}
