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

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "graalvm-js"
    name = "Elide JS integration package for GraalVM"
    description = "Integration package with GraalVM, Elide, and JS."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }

  checks {
    // Broken horribly.
    spotless = false
  }
}

val enableEdgeJvm = false
val edgeVersion = "25x"
val baseVersion = "24x"
val effectivePatchedVersion = if (enableEdgeJvm) edgeVersion else baseVersion

sourceSets {
  main {
    if (enableEdgeJvm) {
      kotlin.srcDirs("src/main/kotlin$edgeVersion")
    } else {
      kotlin.srcDirs("src/main/kotlin$baseVersion")
    }
  }
}

val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")

val patchedLibs = files(
  gvmJarsRoot.file("graaljs-$effectivePatchedVersion.jar"),
  gvmJarsRoot.file("truffle-api-$effectivePatchedVersion.jar"),
)

val patchedDependencies: Configuration by configurations.creating { isCanBeResolved = true }

dependencies {
  annotationProcessor(libs.graalvm.truffle.processor)
  api(projects.packages.engine)
  api(patchedLibs)
  api(libs.graalvm.truffle.api)
  implementation(libs.kotlinx.atomicfu)
  patchedDependencies(patchedLibs)
}

configurations.all {
  listOf(
    libs.graalvm.js.language,
    libs.graalvm.truffle.api,
  ).forEach {
    it.get().let {
      exclude(group = it.group, module = it.name)
    }
  }
}

graalvmNative {
  agent {
    enabled = false
  }
}
