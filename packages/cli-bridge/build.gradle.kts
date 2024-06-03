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
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import elide.internal.conventions.kotlin.KotlinTarget

plugins {
  java
  `java-library`
  distribution
  publishing
  jacoco
  `jvm-test-suite`
  `maven-publish`

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.gradle.checksum)
  id(libs.plugins.shadow.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
  alias(libs.plugins.elide.conventions)
}

elide {
  kotlin {
    target = KotlinTarget.JVM
    kotlinVersionOverride = "2.0"
    explicitApi = true
  }

  docs {
    enabled = false
  }
}

dependencies {
  api(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
}

val isRelease = properties["elide.buildMode"] == "release"
val nativeTargets = rootProject.layout.projectDirectory.dir("targets")
val nativeDebugTargets = rootProject.layout.projectDirectory.dir("targets/debug")
val nativeReleaseTargets = rootProject.layout.projectDirectory.dir("targets/release")

private fun Exec.configureCargo(vararg extraArgs: String) {
  standardOutput = System.out
  executable = "cargo"
  environment("FORCE_COLOR" to "true")
  args("build", "--color=always", *extraArgs)
}

val buildNativeDebug by tasks.registering(Exec::class) {
  group = "build"
  description = "Build native Rust code via Cargo (debug)"
  configureCargo()
}

val buildNativeRelease by tasks.registering(Exec::class) {
  group = "build"
  description = "Build native Rust code via Cargo (release)"
  executable = "cargo"
  configureCargo("--release")
}

val releaseNatives by tasks.registering {
  outputs.dir(nativeReleaseTargets)
  outputs.upToDateWhen {
    nativeReleaseTargets.asFile.exists()
  }
}

val debugNatives by tasks.registering {
  outputs.dir(nativeDebugTargets)
  outputs.upToDateWhen {
    nativeDebugTargets.asFile.exists()
  }
}

val nativeDeps = if (isRelease) releaseNatives else debugNatives

tasks.jar {
  dependsOn(nativeDeps)
}

tasks.build {
  dependsOn(nativeDeps, tasks.jar)
}
