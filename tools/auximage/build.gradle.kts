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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
  `java-library`
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("elide.internal.conventions")
  id(libs.plugins.graalvm.get().pluginId)
}

group = "dev.elide.tools"
version = rootProject.version as String

elide {
  jvm {
    target = JVM_21
  }

  checks {
    detekt = false
    diktat = false
  }
}

// Java Launcher (GraalVM at either EA or LTS)
val enableEdge = true
val enablePython = true
val enableRuby = false
val enableJvm = false
val enableLlvm = false
val edgeJvmTarget = 22
val ltsJvmTarget = 21
val edgeJvm = JavaVersion.toVersion(edgeJvmTarget)
val ltsJvm = JavaVersion.toVersion(ltsJvmTarget)
val selectedJvmTarget = if (enableEdge) edgeJvmTarget else ltsJvmTarget
val selectedJvm = if (enableEdge) edgeJvm else ltsJvm
val entrypoint = "elide.tools.auximage.MainKt"
val jvmType: JvmVendorSpec = JvmVendorSpec.matching("Oracle Corporation")

val gvmLauncher = javaToolchains.launcherFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

val gvmCompiler = javaToolchains.compilerFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

dependencies {
  implementation(projects.packages.base)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)

  // GraalVM
  implementation(libs.graalvm.polyglot)
  compileOnly(libs.graalvm.svm)
  compileOnly(libs.graalvm.truffle.enterprise)

  // Language: JavaScript
  implementation(libs.graalvm.js.language)
  implementation(libs.graalvm.polyglot.js)

  // Language: WASM
  implementation(libs.graalvm.wasm.language)
  implementation(libs.graalvm.polyglot.wasm)

  // Language: Python
  if (enablePython) {
    implementation(libs.graalvm.python.language.enterprise)
    implementation(libs.graalvm.polyglot.python)
  }

  // Language: Ruby
  if (enableRuby) {
    implementation(libs.graalvm.ruby.language)
    implementation(libs.graalvm.polyglot.ruby)
  }

  // Language: Java
  if (enableJvm) {
    implementation(libs.graalvm.espresso.language)
    implementation(libs.graalvm.polyglot.java)
  }

  // Language: LLVM
  if (enableLlvm) {
    implementation(libs.graalvm.llvm.language.enterprise)
    implementation(libs.graalvm.polyglot.llvm)
  }

  api(libs.slf4j)
  runtimeOnly(mn.logback.core)
  runtimeOnly(mn.logback.classic)
}

tasks {
  compileJava {
    javaCompiler = gvmCompiler
  }

  compileTestJava {
    javaCompiler = gvmCompiler
  }

  test {
    javaLauncher = gvmLauncher
  }

  val run: TaskProvider<Exec> by registering(Exec::class) {
    group = "Execution"
    description = "Run the auxiliary image generator tool."
    dependsOn(nativeCompile)
    executable = layout.buildDirectory.file("native/nativeCompile/auximage").get().asFile.path.toString()
  }
}

fun nativeImageArgs(): List<String> = listOf(
  "-O1",
  "-J-Xmx24g",
  "--gc=epsilon",
  "-Dpolyglotimpl.DisableVersionChecks=true",
  "-J-Dpolyglotimpl.DisableVersionChecks=true",
  "--initialize-at-build-time=",
  "--exclude-config", "python-language-${libs.versions.graalvm.pin.get()}.jar", "META-INF\\/native-image\\/.*.properties",
  "-H:+AuxiliaryEngineCache",
  "-H:+UseCompressedReferences",
  "-H:+ReportExceptionStackTraces",
  "-H:ReservedAuxiliaryImageBytes=1073741824",
)

graalvmNative {
  useArgFile = true
  testSupport = false
  toolchainDetection = false

  binaries {
    named("main") {
      mainClass = entrypoint
      //javaLauncher = gvmLauncher
      fallback = false
      sharedLibrary = false
      richOutput = true
      imageName = "auximage"
      buildArgs(nativeImageArgs())
    }
  }
}

tasks.detekt {
  jvmTarget = "21"
}
