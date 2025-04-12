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
@file:Suppress("UnstableApiUsage")

import elide.internal.conventions.publishing.publish
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `java-library`
  alias(libs.plugins.elide.conventions)
}

elide {
  checks {
    spotless = false
    checkstyle = false
    detekt = false
  }

  jvm {
    alignVersions = true
    target = JvmTarget.JVM_22
  }

  publishing {
    id = "transport-uring"
    name = "Elide Transport: Uring"
    description = "Netty native transport support via uring."

    publish("maven") {
      from(components["java"])
    }
  }
}

java {
  withSourcesJar()
  withJavadocJar()
}

dependencies {
  api(projects.packages.transport.transportCommon)
}

tasks.compileJava {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    listOf(
      "-nowarn",
      "-Xlint:none",
    )
  })
}
