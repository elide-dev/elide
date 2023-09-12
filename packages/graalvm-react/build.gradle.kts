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
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  id("elide.internal.conventions")
}

val buildWasm = project.properties["buildWasm"] == "true"

elide {
  publishing {
    id = "graalvm-react"
    name = "Elide React integration for GraalJs"
    description = "Integration package with GraalVM and GraalJS."
  }

  kotlin {
    target = KotlinTarget.JsNode.let {
      if(buildWasm) it + KotlinTarget.WASM else it
    }
  }
}

dependencies {
  js {
    api(npm("esbuild", libs.versions.npm.esbuild.get()))
    api(npm("prepack", libs.versions.npm.prepack.get()))
    api(npm("buffer", libs.versions.npm.buffer.get()))
    api(npm("readable-stream", libs.versions.npm.stream.get()))
    api(npm("typescript", libs.versions.npm.typescript.get()))
    api(npm("@mui/system", libs.versions.npm.mui.get()))

    implementation(projects.packages.graalvmJs)

    implementation(libs.kotlinx.wrappers.node)
    implementation(libs.kotlinx.wrappers.mui)
    implementation(libs.kotlinx.wrappers.react)
    implementation(libs.kotlinx.wrappers.react.dom)
    implementation(libs.kotlinx.wrappers.react.router.dom)
    implementation(libs.kotlinx.wrappers.remix.run.router)
    implementation(libs.kotlinx.coroutines.core.js)
    implementation(libs.kotlinx.serialization.core.js)
    implementation(libs.kotlinx.serialization.json.js)
    implementation(libs.kotlinx.wrappers.css)
    implementation(libs.kotlinx.wrappers.emotion)
    implementation(libs.kotlinx.wrappers.browser)
    implementation(libs.kotlinx.wrappers.history)
    implementation(libs.kotlinx.wrappers.typescript)
  }

  jsTest {
    implementation(projects.packages.test)
  }
}
