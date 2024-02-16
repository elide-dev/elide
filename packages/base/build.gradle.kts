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

import elide.internal.conventions.kotlin.*

plugins {
  id("elide.internal.conventions")
  kotlin("multiplatform")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
}

elide {
  publishing {
    id = "base"
    name = "Elide Base"
    description = "Baseline logic and utilities which are provided for most supported Kotlin and Elide platforms."
  }

  kotlin {
    target = KotlinTarget.All
    atomicFu = true
    explicitApi = true
  }

  checks {
    detekt = true
  }
}

kotlin {
  sourceSets {
    val nativeMain by getting {
      dependencies {
        // fix: KT-64111. Remove when fixed.
        implementation(libs.kotlinx.atomicfu)
      }
    }
  }
}

dependencies {
  common {
    api(projects.packages.core)
    api(libs.kotlinx.atomicfu)
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.io)
    api(libs.kotlinx.io.bytestring)
    api(libs.kotlinx.serialization.core)

    api(libs.elide.uuid)
    implementation(kotlin("stdlib"))
  }

  commonTest {
    implementation(kotlin("test"))
    implementation(projects.packages.test)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
  }

  jvm {
    api(libs.slf4j)
    api(libs.jakarta.inject)
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
    api(mn.micronaut.context)

    api(libs.commons.compress)
    api(libs.commons.codec)

    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlinx.coroutines.jdk9)
    implementation(libs.kotlinx.coroutines.slf4j)
  }

  jvmTest {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiter)

    runtimeOnly(libs.junit.jupiter.engine)
    runtimeOnly(libs.logback)
  }

  js {
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)

    implementation(libs.kotlinx.serialization.json)
  }

  wasi {
    api(npm("@js-joda/core", libs.versions.npm.joda.get()))
  }

  native {
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
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
