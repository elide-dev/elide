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

import elide.internal.conventions.elide
import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "test"
    name = "Elide Test"
    description = "Universal testing utilities in every language supported by Kotlin and Elide."
  }
  
  kotlin {
    target = KotlinTarget.All
    explicitApi = true
  }
}

kotlin {
  js {
    browser {
      testTask(Action {
        useKarma {
          this.webpackConfig.experiments.add("topLevelAwait")
          useChromeHeadless()
          useConfigDirectory(project.projectDir.resolve("karma.config.d").resolve("wasm"))
        }
      })
    }
  }
}

dependencies {
  jvm {
    implementation(projects.packages.server)
    api(kotlin("stdlib-jdk8"))
    api(kotlin("test-junit5"))
    api(libs.jakarta.inject)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.coroutines.jdk9)
    api(mn.micronaut.context)
    api(mn.micronaut.runtime)
    api(mn.micronaut.test.junit5)
    api(mn.micronaut.http)
    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.params)

    implementation(libs.protobuf.java)
    implementation(libs.protobuf.util)
    implementation(libs.protobuf.kotlin)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.grpc.testing)
    implementation(libs.jsoup)

    implementation(libs.truth)
    implementation(libs.truth.java8)
    implementation(libs.truth.proto)

    implementation(mn.micronaut.http.client)
    implementation(mn.micronaut.http.server)

    runtimeOnly(libs.junit.jupiter.engine)
    runtimeOnly(libs.logback)
  }

  js {
    // KT-57235: fix for atomicfu-runtime error
    api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")
    api(kotlin("stdlib-js"))
    api(kotlin("test"))
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.coroutines.core.js)
  }
}

tasks {
  withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
      configureEach {
        includes.from("module.md")
      }
    }
  }
}
