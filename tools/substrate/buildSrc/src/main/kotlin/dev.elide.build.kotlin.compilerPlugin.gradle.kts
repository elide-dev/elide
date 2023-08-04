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

import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`

  kotlin("jvm")
  kotlin("kapt")
  id("com.github.gmazzo.buildconfig")
  id("io.gitlab.arturbosch.detekt")
  id("dev.elide.build.substrate")
}

group = "dev.tools.compiler.plugin"
version = rootProject.version as String

java {
  sourceCompatibility = JavaVersion.VERSION_20
  targetCompatibility = JavaVersion.VERSION_20
}

kotlin {
  explicitApi()

  sourceSets.all {
    languageSettings.apiVersion = ElideSubstrate.KOTLIN_VERSION
    languageSettings.languageVersion = ElideSubstrate.KOTLIN_VERSION
  }
}

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = ElideSubstrate.API_VERSION
    languageVersion = ElideSubstrate.KOTLIN_VERSION
    jvmTarget = "20"
    javaParameters = true
    allWarningsAsErrors = true
    incremental = true
    freeCompilerArgs = freeCompilerArgs.plus(listOf(
      "-Xallow-unstable-dependencies",
    ))
  }
}

detekt {
  parallel = true
  ignoreFailures = true
  config.from(rootProject.files("../../config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
  // Target version of the generated JVM bytecode. It is used for type resolution.
  jvmTarget = "18"
}
