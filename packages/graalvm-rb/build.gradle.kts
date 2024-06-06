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
  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  id(libs.plugins.shadow.get().pluginId)

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")

  alias(libs.plugins.elide.conventions)
}

// Enable LLVM use for Ruby.
val enableLlvm = false

elide {
  publishing {
    id = "graalvm-rb"
    name = "Elide Ruby for GraalVM"
    description = "Integration package with GraalVM and TruffleRuby."

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
}

val oracleGvm = false
val nativeArgs = listOfNotNull(
  "--shared",
  "--initialize-at-build-time=",
  "-H:+JNIExportSymbols",
  "-H:+SourceLevelDebug",
  "-H:-RemoveUnusedSymbols",
  "-H:-StripDebugInfo",
  "-H:Class=elide.runtime.ruby.ElideRuby",
  "-H:Method=main",
  "-H:+UnlockExperimentalVMOptions",
)

graalvmNative {
  binaries {
    create("shared") {
      sharedLibrary = true
      imageName = "libelideruby"
      buildArgs(nativeArgs)

      classpath(
        tasks.compileJava.get().outputs.files,
        tasks.compileKotlin.get().outputs.files,
        configurations.compileClasspath,
        configurations.runtimeClasspath,
      )
    }

    named("test") {
      fallback = false
      sharedLibrary = false
      quickBuild = true
      buildArgs(nativeArgs)

      classpath(
        tasks.compileJava.get().outputs.files,
        tasks.compileKotlin.get().outputs.files,
        tasks.compileTestJava.get().outputs.files,
        tasks.compileTestKotlin.get().outputs.files,
        configurations.testCompileClasspath,
        configurations.testRuntimeClasspath,
      )
    }
  }
}

fun ExternalModuleDependency.banLlvm() {
  exclude("org.graalvm.llvm", "llvm-language")
  exclude("org.graalvm.llvm", "llvm-language-native")
  exclude("org.graalvm.llvm", "llvm-native-community")
  exclude("org.graalvm.llvm", "llvm-language-nfi")
}

fun ExternalModuleDependency.rubyExclusions() {
  if (!enableLlvm) {
    banLlvm()
  }
}

dependencies {
  api(projects.packages.base)
  api(projects.packages.engine)
  implementation(libs.bundles.graalvm.ruby) { rubyExclusions() }
  implementation(libs.kotlinx.coroutines.core)
  api(libs.graalvm.ruby.language) { rubyExclusions() }
  if (!oracleGvm) {
    api(libs.graalvm.polyglot.ruby.community)
  }

  compileOnly(libs.graalvm.svm)
  if (oracleGvm) {
    compileOnly(libs.graalvm.truffle.enterprise)
  }

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
    enabled = false // @TODO(sgammon): temporary while broken

    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
  }
}
