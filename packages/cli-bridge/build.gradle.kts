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
  alias(libs.plugins.kover)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.shadow)
  alias(libs.plugins.gradle.checksum)

  id("elide.internal.conventions")
}

elide {
  kotlin {
    target = KotlinTarget.JVM
    kotlinVersionOverride = "2.0"
  }

  docs {
    enabled = false
  }
}

dependencies {
  implementation(libs.j4rs)
}
