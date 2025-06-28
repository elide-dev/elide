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
import elide.internal.conventions.kotlin.dependencies
import elide.internal.conventions.kotlin.jvm

plugins {
  kotlin("multiplatform")
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "test"
    name = "Elide Test"
    description = "Universal testing utilities in every language supported by Kotlin and Elide."
  }

  kotlin {
    target = KotlinTarget.Default
    explicitApi = true
  }
}

dependencies {
  jvm {
    api(libs.kotlin.stdlib.jdk8)
    api(libs.kotlin.test.junit5)
    api(libs.jakarta.inject)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.coroutines.jdk9)
    api(mn.micronaut.context)
    api(mn.micronaut.runtime)
    api(mn.micronaut.test.junit5)
    api(mn.micronaut.http)
    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.params)
    api("org.wiremock:wiremock-standalone:3.13.1")

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
}
