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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
  kotlin("kapt")
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val strictMode = project.properties["strictMode"] as? String == "true"

// Compiler: Kotlin
// ----------------
// Settings for compiling Kotlin to JavaScript.
kotlin {
  js {
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

// Sources: Kotlin
// ---------------
// Shared configuration for Kotlin language and compiler.
kotlin {
  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      progressiveMode = false
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }
}

tasks.withType<KotlinCompileCommon>().configureEach {
  kotlinOptions.apply {
    apiVersion = kotlinLanguageVersion
    languageVersion = kotlinLanguageVersion
    freeCompilerArgs = freeCompilerArgs.plus(Elide.jsCompilerArgs).toSortedSet().toList()
    allWarningsAsErrors = strictMode
  }
}
