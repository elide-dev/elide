/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import elide.internal.conventions.kotlin.KotlinTarget
import elide.toolchain.host.TargetInfo

plugins {
  alias(libs.plugins.elide.conventions)
  kotlin("jvm")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
}

elide {
  kotlin {
    atomicFu = true
    target = KotlinTarget.JVM
    explicitApi = true
    customKotlinCompilerArgs += listOf("-Xcontext-receivers")
  }

  checks {
    diktat = false
  }
}

dependencies {
  implementation(libs.kotlinx.atomicfu)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlin.stdlib.jdk8)
  api(projects.packages.core)
  api(projects.packages.base)
  api(libs.jetbrains.annotations)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.jdk8)
  api(libs.kotlinx.coroutines.guava)
  implementation(libs.guava)
  // Needed for ImageInfo.inImageCode() reference in Tracing.kt during JVM builds
  compileOnly(libs.graalvm.svm)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.kotlinx.coroutines.test)
  testRuntimeOnly(libs.logback.core)
  testRuntimeOnly(libs.logback)
}

val elideTarget = TargetInfo.current(project)

tasks.named("test", Test::class) {
  systemProperty("java.library.path", StringBuilder().apply {
    append(rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/debug").asFile.path)
    append(File.pathSeparator)
    append(rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/release").asFile.path)
    System.getProperty("java.library.path", "").let {
      if (it.isNotEmpty()) {
        append(File.pathSeparator)
        append(it)
      }
    }
  }.toString())
}
