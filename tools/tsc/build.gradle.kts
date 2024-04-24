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

import org.gradle.kotlin.dsl.elide
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
  mainClass = "dev.elide.runtime.tooling.tsc.TypeScriptCompiler"
}

elide {
  jvm {
    alignVersions = true
  }

  publishing {
    id = "elide-tsc"
    name = "Elide TypeScript Integration"
    description = "Integration package with GraalVM and TypeScript."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  docs {
    enabled = false
  }
}

dependencies {
  api(libs.graalvm.js.language)
}

tasks.named("run") {
  outputs.cacheIf { false }
}
