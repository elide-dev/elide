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
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Path

plugins {
  `cpp-library`
  `java-library`
  id("elide.internal.conventions")
}

library {
  targetMachines = listOf(
    machines.windows.x86,
    machines.windows.x86_64,
    machines.macOS.x86_64,
    machines.macOS.architecture("arm64"),
    machines.linux.x86_64,
    machines.linux.architecture("arm64"),
  )
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

val jdkHome: String = System.getenv("GRAALVM_HOME")?.ifBlank { null }
  ?: System.getenv("JAVA_HOME")?.ifBlank { null }
  ?: System.getProperty("java.home")

val jdkHomePath: Path = Path.of(jdkHome)
val jdkLibPath: Path = jdkHomePath.resolve("lib")
val jdkIncludePath: Path = jdkHomePath.resolve("include")
val jdkNativeIncludePath: Path = when {
  HostManager.hostIsMac -> jdkIncludePath.resolve("darwin")
  HostManager.hostIsLinux -> jdkIncludePath.resolve("linux")
  HostManager.hostIsMingw -> jdkIncludePath.resolve("windows")
  else -> error("Unsupported OS for native builds")
}

tasks.withType(CppCompile::class) {
  source.from(layout.projectDirectory.dir("src/main/cpp").asFileTree.matching { include("**/*.c") })

  compilerArgs.addAll(listOf(
    "-x", "c",
    "-mmacosx-version-min=11.0",
    "-I$jdkIncludePath",
    "-I$jdkNativeIncludePath",
  ))
}

tasks.withType(LinkSharedLibrary::class.java).configureEach {
  linkerArgs.addAll(listOf(
    "-Wl,-platform_version,macos,11.0,11.0",
    "-L$jdkLibPath",
  ))
}

tasks.processResources {
  val libs = layout.buildDirectory.dir("lib/main/release")
  val compiles = tasks.withType(CppCompile::class)
  val linkages = tasks.withType(LinkSharedLibrary::class)
  val stripped = tasks.withType(StripSymbols::class)
  dependsOn(compiles, linkages, stripped)

  inputs.dir(libs)

  from("build/lib/main/release") {
    exclude("**/stripped/**")
    into("META-INF/native/")
  }
}
