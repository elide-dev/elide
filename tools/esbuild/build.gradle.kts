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

import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.kotlin.dsl.elide
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  java
  application
  alias(libs.plugins.micronaut.library)
  alias(libs.plugins.micronaut.graalvm)

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")

  id("elide.internal.conventions")
}

application {
  mainClass = "dev.elide.runtime.tooling.esbuild.ESBuild"
}

elide {
  publishing {
    id = "elide-esbuild"
    name = "Elide esbuild integration"
    description = "Integration package with GraalVM and esbuild."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }

  docs {
    enabled = false
  }
}

dependencies {
  api(libs.graalvm.wasm.language)
  api(libs.graalvm.js.language)
}

tasks.named("run") {
  outputs.cacheIf { false }
}
