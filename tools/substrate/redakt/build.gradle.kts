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

import GradleProject.projectConstants
import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  distribution
  signing
  `embedded-kotlin`

  id("dev.elide.build")
  id("dev.elide.build.jvm")
  id("com.google.devtools.ksp")
  id("dev.elide.build.kotlin.compilerPlugin")
}

group = "dev.elide.tools.kotlin.plugin"
version = rootProject.version as String

projectConstants(
  packageName = "elide.tools.kotlin.plugin.redakt",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("redakt"),
    "PLUGIN_VERSION" to Constant.string(Elide.version),
  )
)

java {
  sourceCompatibility = JavaVersion.VERSION_20
  targetCompatibility = JavaVersion.VERSION_20
}

publishing {
  elideTarget(
    project,
    label = "Elide Substrate: Redakt Plugin",
    group = "dev.elide.tools.kotlin.plugin",
    artifact = "redakt-plugin",
    summary = "Kotlin compiler plugin for redacting sensitive data from logs and toString.",
  )
}

dependencies {
  ksp(libs.autoService.ksp)
  api(projects.compilerUtil)
  compileOnly(libs.kotlin.compiler.embedded)
  implementation(libs.google.auto.service)
  implementation(kotlin("stdlib"))

  testImplementation(kotlin("test"))
  testImplementation(libs.truth)
  testImplementation(libs.truth.proto)
  testImplementation(libs.truth.java8)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.testing)
  testImplementation(libs.kotlin.compiler.embedded)
  testImplementation(project(":compiler-util", "test"))
}
