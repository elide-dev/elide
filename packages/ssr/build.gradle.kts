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
  kotlin("plugin.serialization")
  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "ssr"
    name = "Elide SSR"
    description = "Package for server-side rendering (SSR) capabilities with the Elide Framework."
  }
  
  kotlin {
    target = KotlinTarget.All
    explicitApi = true
  }
}

val encloseSdk = !System.getProperty("java.vm.version").contains("jvmci")

dependencies {
  common {
    implementation(kotlin("stdlib-common"))
    implementation(projects.packages.base)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
  }

  commonTest {
    implementation(projects.packages.test)
  }

  jvm {
    api(mn.micronaut.http)
    implementation(libs.graalvm.polyglot)
  }

  jvmTest {
    implementation(kotlin("test"))
    implementation(projects.packages.test)
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.params)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    runtimeOnly(libs.junit.jupiter.engine)
  }

  js {
    // KT-57235: fix for atomicfu-runtime error
    api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.9.20")
  }
}
