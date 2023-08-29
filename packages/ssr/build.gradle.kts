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
  "unused_variable",
  "DSL_SCOPE_VIOLATION",
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import ElidePackages.elidePackage

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val buildWasm = project.properties["buildWasm"] == "true"

kotlin {
  explicitApi()

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
  jvm()
  if (buildWasm) wasm {
    browser()
    nodejs()
    d8()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(projects.packages.base)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(projects.packages.test)
      }
    }
    val jvmMain by getting {
      dependencies {
        api(mn.micronaut.http)
        compileOnly(libs.graalvm.sdk)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(projects.packages.test)
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
        runtimeOnly(libs.junit.jupiter.engine)
      }
    }
    val jsMain by getting {
      dependencies {
        // KT-57235: fix for atomicfu-runtime error
        api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")
      }
    }
    val jsTest by getting
  }
}

elidePackage(
  id = "ssr",
  name = "Elide SSR",
  description = "Package for server-side rendering (SSR) capabilities with the Elide Framework.",
)
