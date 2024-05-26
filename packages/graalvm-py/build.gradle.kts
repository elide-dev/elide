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
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  alias(libs.plugins.micronaut.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.shadow)

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "graalvm-py"
    name = "Elide Python for GraalVM"
    description = "Integration package with GraalVM and GraalPy."

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

  native {
    target = NativeTarget.LIB
    useAgent = false
  }

  checks {
    // Needs suppressions for inlined files from GVM (`JNIFeature.kt` and `BouncyCastleFeature.kt`)
    diktat = false
    detekt = false
  }
}

val oracleGvm = false
val nativeArgs = listOfNotNull(
  "--shared",
  "--initialize-at-build-time=",
  "-H:+JNIExportSymbols",
  "-H:+SourceLevelDebug",
  "-H:-RemoveUnusedSymbols",
  "-H:-StripDebugInfo",
)

graalvmNative {
  binaries {
    create("shared") {
      sharedLibrary = true
      imageName = "libelidepython"
      classpath(tasks.compileJava, tasks.compileKotlin, configurations.nativeImageClasspath)
      buildArgs(nativeArgs)
    }

    named("test") {
      fallback = false
      sharedLibrary = false
      quickBuild = true
      buildArgs(nativeArgs)
    }
  }
}

dependencies {
  api(projects.packages.engine)
  api(libs.graalvm.truffle.nfi.libffi)
  api(libs.graalvm.python.language)
  api(libs.graalvm.python.resources)
  api(libs.graalvm.python.embedding)

  if (oracleGvm) {
    api(libs.graalvm.python.language.enterprise)
  } else {
    api(libs.graalvm.polyglot.python.community)
  }

  implementation(libs.kotlinx.coroutines.core)

  compileOnly(libs.graalvm.svm)
  if (oracleGvm) compileOnly(libs.graalvm.truffle.enterprise)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvm)
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

tasks {
  jar.configure {
    exclude("**/runtime.current.json")
  }

  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
    systemProperty("elide.js.vm.enableStreams", "true")
  }
}
