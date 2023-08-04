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

@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js.node")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  sourceSets {
    val jsMain by getting {
      dependencies {
        api(npm("esbuild", libs.versions.npm.esbuild.get()))
        api(npm("prepack", libs.versions.npm.prepack.get()))
        api(npm("buffer", libs.versions.npm.buffer.get()))
        api(npm("readable-stream", libs.versions.npm.stream.get()))
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
    }

    val jsTest by getting {
      dependencies {
        implementation(projects.packages.test)
      }
    }
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("graalvm-react", "elide-graalvm-react")

    pom {
      name = "Elide React integration for GraalJS"
      url = "https://elide.dev"
      description = "Integration package with GraalVM and GraalJS."

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}

val enableSigning: String? by properties
if (enableSigning == "true") {
  afterEvaluate {
    listOf(
      "publishKotlinMultiplatformPublicationToElideRepository" to "signKotlinMultiplatformPublication",
    ).forEach {
      tasks.named(it.first).configure {
        dependsOn(it.second)
      }
    }
  }
}
