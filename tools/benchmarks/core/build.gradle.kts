/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*

plugins {
  kotlin("kapt")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")
  kotlin("multiplatform")

  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

val javaLanguageVersion = project.properties["versions.java.language"] as String

kotlin {
  jvm()
}

sourceSets.all {
//  kotlin.srcDirs(listOf("jmh/src"))
//  resources.srcDirs(listOf("jmh/resources"))
}

dependencies {
  implementation(libs.kotlinx.benchmark.runtime)
  implementation(projects.packages.core)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
  configurations {
//    named("main") {
//      warmups = 10
//      iterations = 5
//    }
  }
  targets {
//    register("main") {
//      this as JvmBenchmarkTarget
//      jmhVersion = libs.versions.jmh.lib.get()
//    }
  }
}

tasks.withType(Jar::class).configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType(Copy::class).configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = javaLanguageVersion
    javaParameters = true
    incremental = true
  }
}
