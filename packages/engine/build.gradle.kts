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
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

group = "dev.elide"

elide {
  publishing {
    id = "engine"
    name = "Elide Runtime Engine"
    description = "Core package for internal runtime use."

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
  api(mn.micronaut.core)
  api(libs.graalvm.polyglot)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)

  testApi(projects.packages.base)
  testApi(projects.packages.graalvm)
  testAnnotationProcessor(mn.micronaut.inject.java)
}

val testInternals by configurations.registering {
  extendsFrom(configurations.testImplementation.get())
  isCanBeConsumed = true
  isCanBeResolved = false
}

val testJar by tasks.registering(Jar::class) {
  archiveBaseName.set("engine-test")
  from(sourceSets.test.get().output)
}

artifacts {
  add(testInternals.name, testJar)
}
