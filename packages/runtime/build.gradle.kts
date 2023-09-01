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
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
  "OPT_IN_USAGE",
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import ElidePackages.elidePackage

plugins {
  kotlin("plugin.noarg")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  id("dev.elide.internal.kotlin.redakt")
  id("dev.elide.build.multiplatform")
  id("dev.elide.build.publishable")
}

group = "dev.elide"

val buildMingw = project.properties["buildMingw"] == "true"
val buildWasm = project.properties["buildWasm"] == "true"

kotlin {
  explicitApi()

  jvm {
    compilations.all {
      kotlinOptions {
        apiVersion = libs.versions.kotlin.language.get()
        languageVersion = libs.versions.kotlin.language.get()
      }
    }
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }
  js {
    browser()
    nodejs()
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
    browser()
    nodejs()
    d8()
  }

  macosArm64()
  iosArm64()
  iosX64()
  watchosArm32()
  watchosArm64()
  watchosX64()
  tvosArm64()
  tvosX64()
  if (buildMingw) mingwX64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
      }
    }
    val commonTest by getting {
      dependencies {
        //
      }
    }
    val jvmMain by getting {
      dependencies {
        //
      }
    }
    val jvmTest by getting {
      dependencies {
        //
      }
    }
    val jsMain by getting {
      dependencies {
        //
      }
    }
    val jsTest by getting
    val nativeMain by getting {
      dependencies {
        //
      }
    }
    val nativeTest by getting

    if (buildMingw) {
      val mingwX64Main by getting { dependsOn(nativeMain) }
    }
    val macosArm64Main by getting { dependsOn(nativeMain) }
    val iosArm64Main by getting { dependsOn(nativeMain) }
    val iosX64Main by getting { dependsOn(nativeMain) }
    val watchosArm32Main by getting { dependsOn(nativeMain) }
    val watchosArm64Main by getting { dependsOn(nativeMain) }
    val watchosX64Main by getting { dependsOn(nativeMain) }
    val tvosArm64Main by getting { dependsOn(nativeMain) }
    val tvosX64Main by getting { dependsOn(nativeMain) }
    if (buildWasm) {
      val wasmMain by getting { dependsOn(commonMain) }
      val wasmTest by getting { dependsOn(commonTest) }
    }
  }
}

elidePackage(
  id = "runtime",
  name = "Elide Runtime",
  description = "Runtime context and API definitions.",
) {
  java9Modularity = false
}
