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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  publishing

  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

val defaultJavaVersion = "17"
val defaultKotlinVersion = "1.9"

val strictMode = project.properties["strictMode"] as? String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as? String == "true"
val kotlinVersion = project.properties["versions.kotlin.sdk"] as? String
val javaLanguageVersion = project.properties["versions.java.language"] as? String ?: defaultJavaVersion
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageTarget
    javaParameters = true
    freeCompilerArgs = freeCompilerArgs.plus(Elide.kaptCompilerArgs).toSortedSet().toList()
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageTarget)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

kotlin {
  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      progressiveMode = true
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }
}

// Tool: All-Open
// --------------
// Designates annotations which mark classes that should be open by default.
allOpen {
  annotation("io.micronaut.aop.Around")
}

// Tool: No-Arg
// ------------
// Designates annotations which mark classes for synthesized no-argument constructors.
noArg {
  annotation("elide.annotations.Model")
}

// Tool: Kover
// -----------
// Settings for Kotlin coverage.
extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverReportExtension> {
  defaults {
    xml {
      //  generate an XML report when running the `check` task
      onCheck = properties["elide.ci"] == "true"
    }
  }
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin" && requested.name.contains("stdlib")) {
      useVersion(kotlinVersion ?: "1.9.20-Beta")
      because("pin kotlin stdlib")
    }
  }
}
