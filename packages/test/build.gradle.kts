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

import ElidePackages.elidePackage

plugins {
  id("dev.elide.build.multiplatform")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String
val buildMingw = project.properties["buildMingw"] == "true"

kotlin {
  explicitApi()

  js {
    nodejs()
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

  jvm {
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }

  wasm {
    browser {
      testTask(Action {
        useKarma {
          this.webpackConfig.experiments.add("topLevelAwait")
          useChromeHeadless()
          useConfigDirectory(project.projectDir.resolve("karma.config.d").resolve("wasm"))
        }
      })
    }
  }

  sourceSets.all {
    languageSettings.apply {
      languageVersion = libs.versions.kotlin.language.get()
      apiVersion = libs.versions.kotlin.language.get()
      optIn("kotlin.ExperimentalUnsignedTypes")
      progressiveMode = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(kotlin("stdlib-common"))
        api(kotlin("test"))
        api(kotlin("test-annotations-common"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(projects.packages.server)
        api(kotlin("stdlib-jdk8"))
        api(kotlin("test-junit5"))
        api(libs.jakarta.inject)
        api(libs.kotlinx.coroutines.test)
        api(libs.kotlinx.coroutines.jdk9)
        api(libs.micronaut.context)
        api(libs.micronaut.runtime)
        api(libs.micronaut.test.junit5)
        api(libs.micronaut.http)
        api(libs.junit.jupiter.api)
        api(libs.junit.jupiter.params)

        implementation(libs.protobuf.java)
        implementation(libs.protobuf.util)
        implementation(libs.protobuf.kotlin)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.guava)
        implementation(libs.grpc.testing)
        implementation(libs.jsoup)

        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(libs.truth.proto)

        implementation(libs.micronaut.http.client)
        implementation(libs.micronaut.http.server)

        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jvmTest by getting
    val jsMain by getting {
      dependencies {
        // KT-57235: fix for atomicfu-runtime error
        api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")
        api(kotlin("stdlib-js"))
        api(kotlin("test"))
        api(libs.kotlinx.coroutines.test)
        api(libs.kotlinx.coroutines.core.js)
      }
    }
    val jsTest by getting
    val nativeMain by getting {
      dependencies {
        api(kotlin("stdlib"))
      }
    }
    val nativeTest by getting {
      dependencies {
        api(kotlin("stdlib"))
        api(kotlin("test"))
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
    val wasmMain by getting {
      dependsOn(nativeMain)
      dependencies {
        implementation(kotlin("stdlib-wasm"))
      }
    }
  }
}

elidePackage(
  id = "test",
  name = "Elide Test",
  description = "Universal testing utilities in every language supported by Kotlin and Elide.",
) {
  java9Modularity = false
}
