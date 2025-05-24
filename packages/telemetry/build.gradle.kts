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

import elide.internal.conventions.kotlin.KotlinTarget

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.ksp)
  alias(libs.plugins.elide.conventions)
}

elide {
  kotlin {
    target = KotlinTarget.JVM
    ksp = true
    explicitApi = true
  }
}

val elideVersion: String = libs.versions.elide.bin.get()

buildConfig {
  packageName = "elide.runtime.telemetry.gen"
  useKotlinOutput()
  buildConfigField("ELIDE_VERSION", "\"$elideVersion\"")
}

dependencies {
  ksp(mn.micronaut.inject.kotlin)
  api(projects.packages.base)
  api(projects.packages.core)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.coroutines.reactive)
  api(libs.kotlinx.serialization.core)
  api(libs.kotlinx.serialization.json)
  api(libs.kotlinx.serialization.protobuf)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.json.core)
  implementation(mn.micronaut.serde.api)
  implementation(mn.micronaut.serde.jackson)
  implementation(mn.micronaut.http.client)
  implementation(mn.micronaut.http.client.core)
  implementation(mn.micronaut.http.client.jdk)
}
