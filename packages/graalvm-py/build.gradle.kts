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
  kotlin("jvm")
  alias(libs.plugins.micronaut.graalvm)
  id(libs.plugins.shadow.get().pluginId)
  alias(libs.plugins.ksp)
  alias(libs.plugins.elide.conventions)
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
    ksp = true
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

kover {
  currentProject {
    instrumentation {
      excludedClasses.addAll(listOf(
        "elide.runtime.plugins.python.features.JNIFeature",
        "elide.runtime.plugins.python.features.BouncyCastleFeature",
      ))
    }
  }
  reports {
    filters {
      excludes {
        packages("elide.runtime.plugins.python.features")
      }
    }
  }
}

val oracleGvm = true
val enableEdge = false
val enableToolchains = true

val pluginApiHeader: File =
  rootProject.layout.projectDirectory.file("crates/substrate/headers/elide-plugin.h").asFile

if (!pluginApiHeader.exists()) {
  error("Failed to locate plugin API header: '$pluginApiHeader'")
}

val nativeArgs = listOfNotNull(
  "-Delide.natives.pluginApiHeader=$pluginApiHeader",
  "-H:+SourceLevelDebug",
  "-H:-JNIExportSymbols",
  "-H:-RemoveUnusedSymbols",
  "-H:-StripDebugInfo",
  "-H:+JNIEnhancedErrorCodes",
  "-H:+JNIVerboseLookupErrors",
  "-H:+UnlockExperimentalVMOptions",
  "-H:+ReportExceptionStackTraces",
)

// Java Launcher (GraalVM at either EA or LTS)
val edgeJvmTarget = 25
val stableJvmTarget = 25
val edgeJvm = JavaVersion.toVersion(edgeJvmTarget)
val stableJvm = JavaVersion.toVersion(stableJvmTarget)
val selectedJvmTarget = if (enableEdge) edgeJvmTarget else stableJvmTarget
val selectedJvm = if (enableEdge) edgeJvm else stableJvm

val jvmType: JvmVendorSpec =
  if (oracleGvm) JvmVendorSpec.matching("Oracle Corporation") else JvmVendorSpec.GRAAL_VM

val gvmLauncher = javaToolchains.launcherFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

val gvmCompiler = javaToolchains.compilerFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

val layerOut = layout.buildDirectory.file("native/nativeLayerCompile/elide-python.nil")
val baseLayer = project(":packages:graalvm").layout.buildDirectory.file("native/nativeLayerCompile/elide-graalvm.nil")

graalvmNative {
  binaries {
    create("shared") {
      imageName = "libelidepython"
      classpath(tasks.compileJava, tasks.compileKotlin, configurations.nativeImageClasspath)
      buildArgs(nativeArgs.plus(listOf(
        "--shared",
      )))
    }

    create("layer") {
      classpath(tasks.compileJava, tasks.compileKotlin, configurations.nativeImageClasspath)
      buildArgs(nativeArgs.plus(listOf(
        "-H:LayerUse=${baseLayer.get().asFile.absolutePath}",
        "-H:LayerCreate=${layerOut.get().asFile.name}"
      )))
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
  ksp(mn.micronaut.inject.kotlin)
  api(projects.packages.engine)
  api(libs.graalvm.truffle.nfi.libffi)
  api(libs.graalvm.python.language)
  api(libs.graalvm.python.resources)
  api(libs.graalvm.python.embedding)
  api(libs.graalvm.polyglot.python.community)
  implementation(libs.kotlinx.coroutines.core)

  compileOnly(libs.graalvm.svm)
  compileOnly(libs.graalvm.truffle.enterprise)
  compileOnly(libs.graalvm.truffle.runtime.svm)

  // Testing
  testAnnotationProcessor(mn.micronaut.inject.java)
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvm)
  testApi(project(":packages:engine", configuration = "testInternals"))
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

tasks {
  jar.configure {
    exclude("**/runtime.current.json")
  }

  compileJava {
    if (enableToolchains) javaCompiler = gvmCompiler
  }

  compileTestJava {
    if (enableToolchains) javaCompiler = gvmCompiler
  }

  named("nativeLayerCompile").configure {
    dependsOn(":packages:graalvm:nativeLayerCompile")
  }

  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
    systemProperty("elide.js.vm.enableStreams", "true")
    if (enableToolchains) javaLauncher = gvmLauncher
    jvmArgs(listOf(
      "--add-modules=jdk.unsupported",
      "--enable-native-access=ALL-UNNAMED"
    ))
  }
}
