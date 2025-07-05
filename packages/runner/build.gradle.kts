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
  kotlin("plugin.serialization")
  alias(libs.plugins.ksp)
  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

group = "dev.elide"

elide {
  publishing {
    id = "runner"
    name = "Elide Runner"
    description = "Logic package for Elide's runner features."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
    ksp = true
  }

  java {
    // disable module-info processing (not present)
    configureModularity = false
  }

  checks {
    diktat = false
  }
}

val elideTarget = TargetInfo.current(project)

dependencies {
  api(mn.micronaut.core)
  api(projects.packages.base)
  api(projects.packages.exec)
  api(projects.packages.tooling)
  api(libs.graalvm.polyglot)
  api(libs.kotlinx.coroutines.core)
  api(libs.guava)

  testApi(projects.packages.base)
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvmJvm)
  testAnnotationProcessor(mn.micronaut.inject.java)
  testImplementation(libs.graalvm.espresso.polyglot)
  testImplementation(libs.graalvm.espresso.language)
}

tasks.test {
  jvmArgs("-Dpolyglot.engine.WarnInterpreterOnly=false")
}
