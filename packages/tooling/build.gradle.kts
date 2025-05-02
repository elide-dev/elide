import elide.internal.conventions.kotlin.KotlinTarget

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
  }
  checks {
    diktat = false
  }
}

dependencies {
  api(libs.kotlin.stdlib.jdk8)
  api(libs.jetbrains.annotations)
  api(projects.packages.exec)
  api(projects.packages.engine)
  api(projects.packages.graalvm)
  api(projects.packages.graalvmJvm)
  api(projects.packages.graalvmJava)
  api(projects.packages.graalvmKt)
  api(libs.sarif4k)
  api(libs.bundles.maven.model)
  implementation(libs.mordant)
  implementation(libs.mordant.coroutines)
  implementation(libs.mordant.markdown)
  implementation(libs.ktoml)
  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.bundles.maven.resolver)

  implementation(libs.bundles.detekt) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    exclude(group = "org.jline", module = "jline")
    exclude(group = "org.jline", module = "jline-console")
    exclude(group = "org.jline", module = "jline-graal")
    exclude(group = "org.jline", module = "jline-native")
    exclude(group = "org.jline", module = "jline-reader")
    exclude(group = "org.jline", module = "jline-style")
    exclude(group = "org.jline", module = "jline-terminal")
    exclude(group = "org.jline", module = "jline-terminal-ffm")
    exclude(group = "org.jline", module = "jline-terminal-jansi")
    exclude(group = "org.jline", module = "jline-terminal-jna")
    exclude(group = "org.jline", module = "jline-terminal-jni")
  }

  testImplementation(libs.bundles.maven.resolver)
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlin.test.junit5)
}
