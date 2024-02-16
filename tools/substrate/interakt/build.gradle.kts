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

import ElideSubstrate.elideTarget
import GradleProject.projectConstants

plugins {
  `maven-publish`
  distribution
  signing

  id("dev.elide.build")
  id("dev.elide.build.jvm")
  id("com.google.devtools.ksp")
  id("dev.elide.build.kotlin.compilerPlugin")
}

group = "dev.elide.tools.kotlin.plugin"
version = rootProject.version as String

projectConstants(
  packageName = "elide.tools.kotlin.plugin.interakt",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("interakt"),
  )
)

publishing {
  elideTarget(
    project,
    label = "Elide Substrate: Interakt Plugin",
    group = "dev.elide.tools.kotlin.plugin",
    artifact = "interakt-plugin",
    summary = "Experimental Kotlin compiler plugin. Coming soon.",
  )
}

dependencies {
  ksp(libs.autoService.ksp)
  api(projects.compilerUtil)
  compileOnly(libs.kotlin.compiler.embedded)
  implementation(libs.google.auto.service)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.testing)
  testImplementation(libs.kotlin.compiler.embedded)
  testImplementation(project(":compiler-util", "test"))
}
