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

import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  `java-platform`
  distribution
  signing
  idea

  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

group = "dev.elide.tools"
version = rootProject.version as String

dependencies {
  constraints {
    // Kotlin.
    api(kotlin("stdlib"))

    api(libs.elide.tools.compilerUtil)
    api(libs.elide.kotlin.plugin.redakt)
  }
}

sonarqube {
  isSkipProject = true
}

publishing {
  elideTarget(
    project,
    label = "Elide Tools: Substrate BOM",
    group = project.group as String,
    artifact = "elide-substrate-bom",
    summary = "BOM for Kotlin compiler plugins and other core project infrastructure.",
    bom = true,
  )
}
