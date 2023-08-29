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
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import ElidePackages.elidePackage
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions


plugins {
  `maven-publish`
  distribution
  signing

  kotlin("multiplatform")
  kotlin("plugin.serialization")

  id("dev.elide.build.multiplatform.jvm")
  id("dev.elide.build.jvm17")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String
val buildWasm = project.properties["buildWasm"] == "true"

configurations {
  // `modelInternalJvm` is the dependency used internally by other Elide packages to access the protocol model. at
  // present, the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternalJvm") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["jvmRuntimeClasspath"])
  }
}

kotlin {
  jvm {
    withJava()
  }
  js {
    browser()
    nodejs()
  }
  if (buildWasm) wasm {
    browser()
    d8()
  }

  sourceSets {
    /**
     * Variant: KotlinX
     */
    val jvmMain by getting {
      dependencies {
        // API
        api(libs.kotlinx.datetime)
        api(projects.packages.proto.protoCore)
        api(projects.packages.core)
        implementation(libs.kotlinx.serialization.core.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)

        // Implementation
        implementation(kotlin("stdlib"))
        implementation(kotlin("stdlib-jdk8"))
        runtimeOnly(kotlin("reflect"))
      }
    }
    val jvmTest by getting {
      dependencies {
        // Testing
        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(projects.packages.test)
        implementation(project(":packages:proto:proto-core", configuration = "testBase"))
      }
    }
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        apiVersion = Elide.kotlinLanguage
        languageVersion = Elide.kotlinLanguage
        allWarningsAsErrors = true

        if (this is KotlinJvmOptions) {
          jvmTarget = javaLanguageTarget
          javaParameters = true
        }
      }
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = true
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks {
  jvmTest {
    useJUnitPlatform()
  }

  artifacts {
    archives(jvmJar)
    add("modelInternalJvm", jvmJar)
  }
}

elidePackage(
  id = "proto-kotlinx",
  name = "Elide Protocol: KotlinX",
  description = "Elide protocol implementation for KotlinX Serialization",
) {
  java9Modularity = false
}
