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
import kotlin.io.path.absolutePathString
import elide.internal.conventions.kotlin.KotlinTarget

plugins {
  alias(libs.plugins.elide.conventions)
  kotlin("jvm")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  alias(libs.plugins.ksp)
}

elide {
  kotlin {
    ksp = true
    atomicFu = true
    target = KotlinTarget.JVM
    explicitApi = true
  }
  jvm {
    // tooling consumers (such as the intellij plugin) may not support the latest bytecode
    target = JvmTarget.JVM_21
  }
  checks {
    diktat = false
  }
}

dependencies {
  ksp(mn.micronaut.inject.kotlin)

  api(projects.packages.base)
  api(projects.packages.core)

  fun ExternalModuleDependency.pklExclusions() {
    exclude("org.pkl-lang", "pkl-server")
    exclude("org.pkl-lang", "pkl-config-java-all")
  }

  api(libs.kotlin.stdlib.jdk8)
  api(libs.jetbrains.annotations)
  api(libs.pkl.core) { pklExclusions() }
  api(libs.pkl.config.java) { pklExclusions() }
  api(libs.pkl.config.kotlin) { pklExclusions() }
  api(libs.bundles.maven.model)
  api(libs.jline.reader)
  api(libs.jline.builtins)
  api(libs.graalvm.truffle.api)

  implementation(libs.ktoml)
  implementation(libs.kotlinx.io.bytestring)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.protobuf)
  implementation(libs.graalvm.polyglot)
  implementation(libs.semver)
  implementation(libs.purl)
  implementation(libs.mcp)

  // Ktor
  implementation(libs.bundles.ktor.server)
  implementation(libs.ktor.server.cio)

  testImplementation(libs.bundles.maven.resolver)
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlin.test.junit5)
}

tasks.processResources {
  from(layout.projectDirectory.dir("src/main/pkl")) {
    into("META-INF/elide/pkl/")
  }
}

tasks.test {
  systemProperty("elide.root", rootProject.layout.projectDirectory.asFile.toPath().absolutePathString())
  jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}
