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
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class,
)

import ElidePackages.elidePackage

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val buildMingw = project.properties["buildMingw"] == "true"

kotlin {
  explicitApi()

  jvm {
    withJava()
  }

  js {
    browser()
    nodejs()
    generateTypeScriptDefinitions()

    compilations["main"].packageJson {
      customField("resolutions", mapOf(
        "jszip" to "3.10.1",
        "node-fetch" to "3.3.2",
        "typescript" to "4.9.5",
      ))
    }

    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
  }
  macosArm64()
  iosArm64()
  iosX64()
  watchosArm32()
  watchosArm64()
  watchosX64()
  tvosArm64()
  tvosX64()

  wasm {
    nodejs()
    d8()
    browser()
  }

  if (buildMingw) mingwX64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        api(projects.packages.core)
        implementation(libs.elide.uuid)
        api(libs.kotlinx.collections.immutable)
        api(libs.kotlinx.datetime)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(kotlin("stdlib"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        api(libs.slf4j)
        api(libs.jakarta.inject)
        api(libs.micronaut.inject.java)
        api(libs.kotlinx.collections.immutable)
        api(libs.kotlinx.datetime)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.jdk9)
        implementation(libs.kotlinx.coroutines.slf4j)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("test-junit5"))
        implementation(libs.junit.jupiter)
        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jsMain by getting {
      dependencies {
        // KT-57235: fix for atomicfu-runtime error
        api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")
        implementation(kotlin("stdlib-js"))
        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        api(libs.kotlinx.collections.immutable)
        api(libs.kotlinx.datetime)
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(kotlin("test"))
      }
    }
    val nativeMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.collections.immutable)
        api(libs.kotlinx.datetime)
      }
    }
    val nativeTest by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("test"))
      }
    }

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
    val wasmMain by getting { dependsOn(commonMain) }
  }
}

elidePackage(
  id = "base",
  name = "Elide Base",
  description = "Baseline logic and utilities which are provided for most supported Kotlin and Elide platforms.",
)

afterEvaluate {
  tasks.named("compileTestDevelopmentExecutableKotlinJs") {
    enabled = false
  }
  tasks.named("compileTestDevelopmentExecutableKotlinWasm") {
    enabled = false
  }
}
