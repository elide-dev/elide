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

plugins {
  alias(libs.plugins.shadow) apply false
  id("dev.elide.buildtools.plugin") apply false
}

buildscript {
  repositories {
    maven("https://maven.pkg.st")

    maven {
      name = "elide-snapshots"
      url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
      }
    }
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("dev.elide")
      }
    }
    maven {
      name = "dokka-dev"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
      content {
        includeGroup("org.jetbrains.dokka")
      }
    }
    maven {
      name = "wasm-dev"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
      content {
        includeGroup("io.ktor")
        includeGroup("org.jetbrains.compose")
        includeGroup("org.jetbrains.compose.compiler")
        includeGroup("org.jetbrains.kotlin")
        includeGroup("org.jetbrains.kotlinx")
      }
    }
    maven {
      name = "compose-dev"
      url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
      content {
        includeGroup("androidx.compose")
        includeGroup("androidx.compose.compiler")
        includeGroup("org.jetbrains.compose")
        includeGroup("org.jetbrains.compose.compiler")
        includeGroup("web")
      }
    }
    mavenCentral()
    google()
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.255-SNAPSHOT")
  }
}
