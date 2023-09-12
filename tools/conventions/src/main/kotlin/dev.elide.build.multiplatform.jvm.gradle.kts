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
  "UNUSED_VARIABLE",
  "UnstableApiUsage",
)

import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("dev.elide.build.core")
  id("org.jetbrains.kotlinx.kover")
}

val defaultJavaVersion = "17"
val defaultKotlinVersion = "1.9"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion
val ecmaVersion = project.properties["versions.ecma.language"] as String
val strictMode = project.properties["strictMode"] as? String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as? String == "true"

extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverReportExtension> {
  defaults {
    xml {
      //  generate an XML report when running the `check` task
      onCheck = properties["elide.ci"] == "true"
    }
  }
}

kotlin {
  jvm {
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }

  if (project.hasProperty("publishMainHostLock") && project.properties["publishMainHostLock"] == "true") {
    val publicationsFromMainHost =
      listOf(jvm(), js()).map { it.name } + "kotlinMultiplatform"

    publishing {
      publications {
        matching { it.name in publicationsFromMainHost }.all {
          val targetPublication = this@all
          tasks.withType<AbstractPublishToMaven>()
            .matching { it.publication == targetPublication }
            .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
        }
      }
    }
  }

  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      progressiveMode = true
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
}

tasks.withType<KotlinCompileCommon>().configureEach {
  kotlinOptions {
    apiVersion = kotlinLanguageVersion
    languageVersion = kotlinLanguageVersion
    freeCompilerArgs = freeCompilerArgs.plus(Elide.mppCompilerArgs).toSortedSet().toList()
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = kotlinLanguageVersion
    languageVersion = kotlinLanguageVersion
    jvmTarget = javaLanguageTarget
    freeCompilerArgs = freeCompilerArgs.plus(Elide.mppCompilerArgs).toSortedSet().toList()
    javaParameters = true
    allWarningsAsErrors = strictMode
    incremental = true
  }
}
