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

import kotlin.io.path.absolutePathString
import elide.internal.conventions.kotlin.KotlinTarget

plugins {
  alias(libs.plugins.elide.conventions)
  kotlin("jvm")
  kotlin("plugin.serialization")
  alias(libs.plugins.ksp)
}

elide {
  kotlin {
    ksp = true
    target = KotlinTarget.JVM
    explicitApi = true
  }
  checks {
    diktat = false
  }
}

dependencies {
  ksp(mn.micronaut.inject.kotlin)

  api(projects.packages.base)
  api(projects.packages.core)

  implementation(libs.kotlinx.io)
  implementation(libs.kotlinx.io.bytestring)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.cbor)

  implementation(libs.bouncycastle)
  implementation(libs.inquirer) {
    exclude(group = "org.jline", module = "jline")
    exclude(group = "org.fusesource.jansi", module = "jansi")
  }

  // Ktor
  implementation(libs.bundles.ktor.client)
  implementation(libs.ktor.client.cio)

  testImplementation(projects.packages.test)
  testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
  systemProperty("elide.root", rootProject.layout.projectDirectory.asFile.toPath().absolutePathString())
  jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}
