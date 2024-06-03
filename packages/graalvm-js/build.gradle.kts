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

import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.kotlin.dependencies
import elide.internal.conventions.kotlin.js
import elide.internal.conventions.kotlin.jsTest

plugins {
  kotlin("multiplatform")
  alias(libs.plugins.elide.conventions)
}

val buildWasm = project.properties["buildWasm"] == "true"

elide {
  publishing {
    id = "graalvm-js"
    name = "Elide JavaScript for GraalVM"
    description = "Integration package with GraalVM and GraalJS."
  }

  kotlin {
    target = KotlinTarget.JsBrowser.let {
      if (buildWasm) it + KotlinTarget.WASM else it
    }

    explicitApi = true
  }
}

dependencies {
  js {
    api(projects.packages.ssr)
    api(projects.packages.core)
    api(projects.packages.base)
    api(npm("esbuild", libs.versions.npm.esbuild.get()))
    api(npm("typescript", libs.versions.npm.typescript.get()))
    api(npm("buffer", libs.versions.npm.buffer.get()))
    api(npm("readable-stream", libs.versions.npm.stream.get()))
    api(npm("web-streams-polyfill", libs.versions.npm.webstreams.get()))
    api(npm("@emotion/css", libs.versions.npm.emotion.core.get()))
    api(npm("@emotion/server", libs.versions.npm.emotion.server.get()))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.wrappers.node)
    implementation(libs.kotlinx.wrappers.emotion)
    implementation(libs.kotlinx.wrappers.history)
    implementation(libs.kotlinx.wrappers.typescript)
    implementation(libs.kotlinx.wrappers.react.router.dom)
    implementation(libs.kotlinx.wrappers.remix.run.router)
  }

  jsTest {
    implementation(projects.packages.test)
  }
}

tasks.named("compileKotlinJs", Kotlin2JsCompile::class.java) {
  compilerOptions {
    freeCompilerArgs.add("-nowarn")
  }
}
