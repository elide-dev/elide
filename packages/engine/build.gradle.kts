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
import elide.internal.conventions.publishing.publish
import elide.toolchain.host.TargetInfo

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  alias(libs.plugins.ksp)
  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

group = "dev.elide"

elide {
  publishing {
    id = "engine"
    name = "Elide Runtime Engine"
    description = "Core package for internal runtime use."

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

  checks {
    diktat = false
  }
}

dependencies {
  api(mn.micronaut.core)
  api(projects.packages.base)
  api(projects.packages.exec)
  api(projects.packages.tooling)
  api(libs.graalvm.polyglot)
  api(libs.kotlinx.coroutines.core)
  api(libs.guava)
  ksp(mn.micronaut.inject.kotlin)
  implementation(libs.kotlinx.atomicfu)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  compileOnly(libs.graalvm.svm)

  testApi(projects.packages.base)
  testImplementation(projects.packages.test)
  testAnnotationProcessor(mn.micronaut.inject.java)
}

val jniDebug = false
val quickbuild = (
  project.properties["elide.release"] != "true" ||
    project.properties["elide.buildMode"] == "dev"
  )
val isRelease = !quickbuild && (
  project.properties["elide.release"] == "true" ||
    project.properties["elide.buildMode"] == "release"
  )
val elideTarget = TargetInfo.current(project)
val nativesType = if (isRelease) "release" else "debug"

val jvmDefs = StringBuilder().apply {
  append(rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/$nativesType").asFile.path)
  System.getProperty("java.library.path", "").let {
    if (it.isNotEmpty()) {
      append(File.pathSeparator)
      append(it)
    }
  }
}.toString().let {
  listOf(
    "-Djava.library.path=$it"
  )
}.plus(
  listOf(
    "-verbose:jni",
    "-Xlog:library=trace",
    "-Xcheck:jni",
  ).takeIf { jniDebug } ?: emptyList()
)

val testInternals by configurations.registering {
  extendsFrom(configurations.testImplementation.get())
  isCanBeConsumed = true
  isCanBeResolved = false
}

val testJar by tasks.registering(Jar::class) {
  archiveBaseName.set("engine-test")
  from(sourceSets.test.get().output)
}

val pluginApiHeader =
  rootProject.layout.projectDirectory.file("crates/substrate/headers/elide-plugin.h").asFile.path

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
val oracleGvm = true
val enableEdge = false
val enableToolchains = true
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

val layerOut = layout.buildDirectory.file("native/nativeLayerCompile/elide-base.nil")

graalvmNative {
  binaries {
    create("shared") {
      imageName = "libelidecore"
      classpath(tasks.compileJava, tasks.compileKotlin, configurations.nativeImageClasspath)
      buildArgs(nativeArgs.plus("--shared"))
    }

    create("layer") {
      imageName = "libelidecore"
      classpath(tasks.compileJava, tasks.compileKotlin, configurations.nativeImageClasspath)
      buildArgs(nativeArgs.plus("-H:LayerCreate=${layerOut.get().asFile.name}"))
    }

    named("test") {
      buildArgs(nativeArgs.plus(jvmDefs).plus(buildString {
        append("--initialize-at-build-time=")

        listOf(
          "org.junit.platform.launcher.core.DiscoveryIssueNotifier",
          "org.junit.platform.launcher.core.HierarchicalOutputDirectoryProvider",
          "org.junit.platform.launcher.core.LauncherDiscoveryResult${'$'}EngineResultInfo",
          "org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor${'$'}MethodInfo",
          "org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor${'$'}ClassInfo",
          "org.junit.jupiter.engine.descriptor.ExclusiveResourceCollector${'$'}1",
          "org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor${'$'}LifecycleMethods",
        ).joinToString(",").let {
          append(it)
        }
      }))
    }
  }
}

tasks.test {
  dependsOn(":packages:graalvm:natives")
  jvmArgs = jvmDefs
  jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}

artifacts {
  add(testInternals.name, testJar)
}
