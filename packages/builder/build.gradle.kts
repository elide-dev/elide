/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.collections.joinToString
import kotlin.collections.plus
import kotlin.io.path.absolutePathString
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  alias(libs.plugins.elide.conventions)
  kotlin("jvm")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  alias(libs.plugins.ksp)
}

elide {
  publishing {
    id = "builder"
    name = "Elide Builder"
    description = "Elide's project builder."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    ksp = true
    atomicFu = true
    target = KotlinTarget.JVM
    explicitApi = true
  }

  checks {
    diktat = false
  }
}

val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")
val googJarsRoot = rootProject.layout.projectDirectory.dir("third_party/google")

val patchedLibs = files(
  gvmJarsRoot.file("truffle-api.jar"),
  gvmJarsRoot.file("truffle-coverage.jar"),
  gvmJarsRoot.file("library-support.jar"),
  gvmJarsRoot.file("svm-driver.jar"),
  googJarsRoot.file("jib-plugins-common.jar"),
  googJarsRoot.file("jib-cli.jar"),
)

val patchedDependencies: Configuration by configurations.creating { isCanBeResolved = true }

dependencies {
  fun ExternalModuleDependency.pklExclusions() {
    exclude("org.pkl-lang", "pkl-server")
    exclude("org.pkl-lang", "pkl-config-java-all")
  }

  compileOnly(patchedLibs)
  patchedDependencies(patchedLibs)

  ksp(mn.micronaut.inject.kotlin)
  api(libs.kotlin.stdlib.jdk8)
  api(libs.jetbrains.annotations)
  api(libs.jline.reader)
  api(libs.jline.builtins)
  api(projects.packages.tooling)
  api(projects.packages.exec)
  api(projects.packages.engine)
  api(projects.packages.graalvm)
  api(projects.packages.graalvmJvm)
  api(projects.packages.graalvmJava)
  api(projects.packages.graalvmKt)
  api(projects.packages.telemetry)
  api(libs.sarif4k)
  api(libs.markdown)
  api(libs.bundles.maven.model)
  api(libs.pkl.core) { pklExclusions() }
  api(libs.pkl.config.java) { pklExclusions() }
  api(libs.pkl.config.kotlin) { pklExclusions() }

  implementation(libs.graalvm.tools.dap)
  implementation(libs.graalvm.tools.chromeinspector)
  implementation(libs.graalvm.tools.profiler)

  // excluded: patched
  // implementation(libs.graalvm.tools.coverage)

  implementation(libs.classgraph)
  implementation(libs.semver)
  implementation(libs.purl)
  implementation(libs.mordant)
  implementation(libs.mordant.coroutines)
  implementation(libs.mordant.markdown)
  implementation(libs.highlights)
  implementation(libs.junit.platform.launcher)
  implementation(libs.junit.jupiter.engine)
  implementation(libs.ktoml)
  implementation(libs.plugin.redacted.core)
  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.kotlin.serialization.embedded)
  implementation(libs.kotlin.powerAssert.embedded)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.protobuf)
  implementation(libs.kotlinx.html)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.minifyHtml)
  implementation(libs.jib.core)
  implementation(libs.bundles.maven.resolver)
  implementation(libs.snakeyaml.core)

  testImplementation(libs.bundles.maven.resolver)
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlin.test.junit5)
}

val isRelease = (
  project.properties["elide.release"] == "true" ||
  project.properties["elide.buildMode"] == "release"
)

val nativesType = if (!isRelease) "debug" else "release"
val archTripleToken = (findProperty("elide.arch") as? String)
  ?: if (System.getProperty("os.arch") == "aarch64") "aarch64" else "x86_64"
val muslTarget = "$archTripleToken-unknown-linux-musl"
val gnuTarget = "$archTripleToken-unknown-linux-gnu"
val enableStatic = findProperty("elide.static") == "true"
val macTarget = "$archTripleToken-apple-darwin"
val winTarget = "$archTripleToken-pc-windows-gnu"

val targetPath: String = when {
  HostManager.hostIsLinux -> when (enableStatic) {
    // if we are targeting a fully static environment (i.e. musl), we need to sub in the target name in the path as we
    // are always technically cross-compiling.
    true -> rootProject.layout.projectDirectory.dir("target/$muslTarget/$nativesType")

    // otherwise, we use gnu's target.
    false -> rootProject.layout.projectDirectory.dir("target/$gnuTarget/$nativesType")
  }.asFile.path

  HostManager.hostIsMac -> rootProject.layout.projectDirectory.dir("target/$macTarget/$nativesType").asFile.path
  HostManager.hostIsMingw -> rootProject.layout.projectDirectory.dir("target/$winTarget/$nativesType").asFile.path
  else -> error("Unsupported platform for target path")
}

tasks.named("test", Test::class) {
  dependsOn(
    ":packages:graalvm:buildRustNativesForHost",
  )
  useJUnitPlatform()

  systemProperty("elide.root", rootProject.layout.projectDirectory.asFile.toPath().absolutePathString())

  systemProperty(
    "java.library.path",
    listOf(
      targetPath,
    ).plus(
      System.getProperty("java.library.path", "").split(File.pathSeparator).filter {
        it.isNotEmpty()
      }
    ).joinToString(
      File.pathSeparator
    )
  )

  jvmArgs(listOf(
    "--enable-native-access=ALL-UNNAMED",
  ))
}
