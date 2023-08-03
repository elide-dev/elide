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

import dev.elide.buildtools.gradle.plugin.BuildMode
import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*
import tools.elide.assets.EmbeddedScriptLanguage

plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  alias(libs.plugins.elide)
  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

val javaLanguageVersion = project.properties["versions.java.language"] as String

sourceSets.all {
  java.setSrcDirs(listOf("jmh/src"))
  resources.setSrcDirs(listOf("jmh/resources"))
}

dependencies {
  implementation(libs.kotlinx.benchmark.runtime)
  implementation(libs.kotlinx.coroutines.test)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject.java.test)
  implementation(libs.micronaut.runtime)
  implementation(libs.elide.graalvm)
  implementation(libs.lmax.disruptor.core)
  compileOnly(libs.graalvm.sdk)
  runtimeOnly(libs.logback)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
  configurations {
    named("main") {
      warmups = 5
      iterations = 5
    }
  }
  targets {
    register("main") {
      this as JvmBenchmarkTarget
      jmhVersion = "1.36"
    }
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
    apiVersion = "1.9"
    languageVersion = "1.9"
    jvmTarget = javaLanguageVersion
    javaParameters = true
    incremental = true
  }
}
