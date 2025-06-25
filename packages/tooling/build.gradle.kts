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
  checks {
    diktat = false
  }
}

val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")
val googJarsRoot = rootProject.layout.projectDirectory.dir("third_party/google")

val patchedLibs = files(
  gvmJarsRoot.file("truffle-coverage.jar"),
  gvmJarsRoot.file("library-support.jar"),
  gvmJarsRoot.file("svm-driver.jar"),
  googJarsRoot.file("jib-plugins-common.jar"),
  googJarsRoot.file("jib-cli.jar"),
)

val patchedDependencies: Configuration by configurations.creating { isCanBeResolved = true }

dependencies {
  fun ExternalModuleDependency.pklExclusions() {
    exclude("org.pkl-lang", "pkl-server")
    exclude("org.pkl-lang", "pkl-config-java-all")
  }

  compileOnly(patchedLibs)
  patchedDependencies(patchedLibs)

  ksp(mn.micronaut.inject.kotlin)
  api(libs.kotlin.stdlib.jdk8)
  api(libs.jetbrains.annotations)
  api(projects.packages.toolingApi)
  api(projects.packages.exec)
  api(projects.packages.engine)
  api(projects.packages.graalvm)
  api(projects.packages.graalvmJvm)
  api(projects.packages.graalvmJava)
  api(projects.packages.graalvmKt)
  api(projects.packages.telemetry)
  api(libs.sarif4k)
  api(libs.bundles.maven.model)
  api(libs.pkl.core) { pklExclusions() }
  api(libs.pkl.config.java) { pklExclusions() }
  api(libs.pkl.config.kotlin) { pklExclusions() }

  implementation(libs.graalvm.tools.dap)
  implementation(libs.graalvm.tools.chromeinspector)
  implementation(libs.graalvm.tools.profiler)

  // excluded: patched
  // implementation(libs.graalvm.tools.coverage)

  implementation(libs.classgraph)
  implementation(libs.semver)
  implementation(libs.purl)
  implementation(libs.mordant)
  implementation(libs.mordant.coroutines)
  implementation(libs.mordant.markdown)
  implementation(libs.highlights)
  implementation(libs.ktoml)
  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.protobuf)
  implementation(libs.jib.core)
  implementation(libs.bundles.maven.resolver)

  testImplementation(libs.bundles.maven.resolver)
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
  systemProperty("elide.root", rootProject.layout.projectDirectory.asFile.toPath().absolutePathString())
}
