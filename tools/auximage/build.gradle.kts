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
  application
  `java-library`
  kotlin("jvm")
  kotlin("plugin.serialization")
  alias(libs.plugins.elide.conventions)
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
val enableToolchains = true
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

application {
  mainClass = "elide.tools.auximage.MainKt"
}

dependencies {
  implementation(projects.packages.base)
  implementation(projects.packages.engine)
  implementation(projects.packages.graalvm)
  implementation(projects.packages.graalvmTs)

  // Native Libraries
  implementation(projects.packages.sqlite)
  implementation(projects.packages.tcnative)
  implementation(projects.packages.terminal)
  implementation(projects.packages.transport.transportEpoll)
  implementation(projects.packages.transport.transportKqueue)
  implementation(projects.packages.transport.transportUring)

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

  val auximage: TaskProvider<Exec> by registering(Exec::class) {
    group = "Execution"
    description = "Run the auxiliary image generator tool."
    dependsOn(nativeCompile)
    executable = layout.buildDirectory.file("native/nativeCompile/auximage").get().asFile.path.toString()
  }
}

val stamp = (project.properties["elide.stamp"] as? String ?: "false").toBooleanStrictOrNull() ?: false
val nativeVersion = if (stamp) {
  libs.versions.elide.asProvider().get()
} else {
  "1.0-dev-${System.currentTimeMillis() / 1000 / 60 / 60 / 24}"
}

val nativesRootTemplate: (String) -> String = { version ->
  "/tmp/elide-runtime/v$version/native"
}

val nativesPath = nativesRootTemplate(nativeVersion)

val initializeAtBuildTime = listOf(
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.base.Equivalence${'$'}Equals",
  "com.google.common.collect.MapMakerInternalMap${'$'}1",
  "com.google.common.collect.MapMakerInternalMap${'$'}EntrySet",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueEntry${'$'}Helper",
  "elide.runtime.lang.typescript.TypeScriptLanguageProvider",
)

fun nativeImageArgs(): List<String> = listOf(
  "-O1",
  "-J-Xmx24g",
  "--gc=serial",
  "--exclude-config", "python-language-${libs.versions.graalvm.pin.get()}.jar", "META-INF\\/native-image\\/.*.properties",
  "-H:+AuxiliaryEngineCache",
  "-H:+UseCompressedReferences",
  "-H:+ReportExceptionStackTraces",
  "-H:ReservedAuxiliaryImageBytes=1073741824",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "-J--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
  "-J--add-opens=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-Delide.staticJni=true",
  "-J-Delide.staticJni=true",
  "-H:NativeLinkerOption=-L$nativesPath",
  "-Delide.natives=$nativesPath",
  "-J-Delide.natives=$nativesPath",
  "-Djna.library.path=$nativesPath",
  "-J-Djna.library.path=$nativesPath",
  "-Djna.boot.library.path=$nativesPath",
  "-J-Djna.boot.library.path=$nativesPath",
  "-Dorg.sqlite.lib.path=$nativesPath",
  "-J-Dorg.sqlite.lib.path=$nativesPath",
  "-Dorg.sqlite.lib.exportPath=$nativesPath",
  "-J-Dorg.sqlite.lib.exportPath=$nativesPath",
  "-Dio.netty.native.workdir=$nativesPath",
  "-J-Dio.netty.native.workdir=$nativesPath",
  "-Dlibrary.jansi.path=$nativesPath",
  "-J-Dlibrary.jansi.path=$nativesPath",
  "-Dlibrary.jline.path=$nativesPath",
  "-J-Dlibrary.jline.path=$nativesPath",
  "--initialize-at-build-time=${initializeAtBuildTime.joinToString(",")}",
)

graalvmNative {
  useArgFile = true
  testSupport = false
  toolchainDetection = false

  binaries {
    named("main") {
      mainClass = entrypoint
      fallback = false
      sharedLibrary = false
      richOutput = true
      imageName = "auximage"
      buildArgs(nativeImageArgs())
      if (enableToolchains) javaLauncher = gvmLauncher
    }
  }
}

tasks.detekt {
  jvmTarget = "21"
}
