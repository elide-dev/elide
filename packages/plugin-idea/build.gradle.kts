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

plugins {
  alias(libs.plugins.elide.conventions)
  alias(libs.plugins.intellij.platform)
  kotlin("jvm")
  id("java")
}

elide {
  jvm {
    // Intellij plugins can only target up to JVM 21
    target = JvmTarget.JVM_21
  }
  kotlin {
    customKotlinCompilerArgs += "-Xskip-prerelease-check"
  }
}

repositories {
  intellijPlatform {
    defaultRepositories()
  }

  maven {
    name = "elide-snapshots"
    url = uri("https://maven.elide.dev")
    content {
      includeGroup("dev.elide")
      includeGroup("org.pkl-lang")
    }
  }

  maven {
    name = "oss-snapshots"
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    content { includeGroup("dev.elide") }
  }

  mavenCentral()
  google()
}

dependencies {
  // manifest and lockfile parsing
  implementation(projects.packages.tooling) {
    // plugins must use the bundled coroutines library
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
  }

  intellijPlatform {
    create("IC", libs.versions.intellij.target.ide.get())
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.kotlin")
    testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
  }
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = libs.versions.intellij.target.build.get()
    }

    changeNotes = "Initial release."
  }
}
