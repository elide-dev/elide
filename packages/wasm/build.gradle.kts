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
@file:Suppress("UnstableApiUsage")

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
}

kotlin {
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
  wasm {
    d8()
    browser()
    nodejs()
  }

  sourceSets.all {
    languageSettings.apiVersion = Elide.kotlinLanguage
    languageSettings.languageVersion = Elide.kotlinLanguage
  }

  sourceSets {
    val wasmMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
      }
    }
    val wasmTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        options.optIn.add("kotlin.wasm.unsafe.UnsafeWasmMemoryApi")
        options.apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
        options.languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
      }
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
