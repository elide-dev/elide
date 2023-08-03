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

plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  maven("https://maven.pkg.st/")
  maven("https://gradle.pkg.st/")
  maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
}

val kotlinVersion = "1.9.0"

dependencies {
  api(libs.elide.tools.conventions)
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.ksp)
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
