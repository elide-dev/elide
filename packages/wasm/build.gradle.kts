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

@file:OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
}

kotlin {
  wasm {
    d8()
    nodejs()
    browser()
  }

  sourceSets {
    val wasmMain by getting
    val wasmTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }

  tasks.withType<KotlinCompile<*>> {
    kotlinOptions {
      freeCompilerArgs += "-opt-in=kotlin.wasm.unsafe.UnsafeWasmMemoryApi"
    }
  }
}

publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("wasm", "elide-wasm")

    pom {
      name = "Elide WASM Runtime"
      url = "https://elide.dev"
      description = "Integration with WASM/WASI for Elide."

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}
