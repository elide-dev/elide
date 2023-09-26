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
  id("dev.elide.buildtools.plugin") apply false
}

buildscript {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
    mavenCentral()
    google()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20-Beta2")
  }
}
