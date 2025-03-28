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

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish
import elide.toolchain.host.TargetInfo

plugins {
  kotlin("jvm")
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "local-ai"
    name = "Elide Local AI Support"
    description = "Support package for local AI features in Elide."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }

  java {
    // disable module-info processing (not present)
    configureModularity = false
  }

  checks {
    diktat = false
  }
}

dependencies {
  api(projects.packages.engine)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.engine)
}

val elideTarget = TargetInfo.current(project)

val quickbuild = (
  properties["elide.release"] != "true" ||
    properties["elide.buildMode"] == "dev"
  )

val isRelease = !quickbuild && (
  properties["elide.release"] == "true" ||
    properties["elide.buildMode"] == "release"
  )

val nativesType = if (isRelease) "release" else "debug"

val nativesPath: String =
  rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/$nativesType")
    .asFile
    .path

val javaLibPath = provider {
  StringBuilder().apply {
    append(nativesPath)
    append(File.pathSeparator)
    System.getProperty("java.library.path", "").let {
      if (it.isNotEmpty()) {
        append(File.pathSeparator)
        append(it)
      }
    }
  }
}

tasks.test {
  environment("ELIDE_TEST", "true")
  systemProperty("elide.test", "true")
  systemProperty("java.library.path", javaLibPath.get())
  dependsOn(":packages:graalvm:buildRustNativesForHost")
}
