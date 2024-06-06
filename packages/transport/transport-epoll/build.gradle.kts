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
  alias(libs.plugins.elide.conventions)
}

elide {
  checks {
    spotless = false
    checkstyle = false
    detekt = false
  }

  publishing {
    id = "transport-epoll"
    name = "Elide Transport: EPoll"
    description = "Packages native EPoll support for Elide/Netty."
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

library {
  linkage = listOf(Linkage.STATIC, Linkage.SHARED)

  targetMachines = listOf(
    machines.linux.x86_64,
  )
}

val jvmApi: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
val apiElements: Configuration by configurations.getting { extendsFrom(jvmApi) }
val compileClasspath: Configuration by configurations.getting { extendsFrom(jvmApi) }

dependencies {
  jvmApi(projects.packages.transport.transportCommon)
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
  else -> error("Unsupported OS for kqueue")
}

tasks.withType(CppCompile::class.java).configureEach {
  group = "build"
  description = "Compile shared library"
  source.from(layout.projectDirectory.dir("src/main/cpp").asFileTree.matching { include("**/*.c") })
  onlyIf { HostManager.hostIsLinux }

  macros["NETTY_BUILD_STATIC"] = "1"
  macros["NETTY_BUILD_GRAALVM"] = "1"

  compilerArgs.addAll(listOf(
    "-x", "c",
    "-O3",
    "-fPIC",
    "-Werror",
    "-fno-omit-frame-pointer",
    "-Wunused-variable",
    "-fvisibility=hidden",
    "-I$jdkIncludePath",
    "-I$jdkNativeIncludePath",
  ))
}

tasks.withType(LinkSharedLibrary::class.java).configureEach {
  group = "build"
  description = "Link shared library"
  onlyIf { HostManager.hostIsLinux }

  linkerArgs.addAll(listOf(
    "-L$jdkLibPath",
  ))
}

tasks.compileJava {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    listOf(
      "-nowarn",
      "-Xlint:none",
    )
  })
}

tasks.withType(CreateStaticLibrary::class.java).configureEach {
  group = "build"
  description = "Create static library"
  onlyIf { HostManager.hostIsLinux }
}

tasks.withType(StripSymbols::class.java).configureEach {
  onlyIf { HostManager.hostIsLinux }
}

tasks.processResources {
  val resources = layout.projectDirectory.dir("src/main/resources")
  val libs = layout.buildDirectory.dir("lib/main/release")
  val compiles = tasks.withType(CppCompile::class)
  val linkages = tasks.withType(LinkSharedLibrary::class)
  val stripped = tasks.withType(StripSymbols::class)
  val statics = tasks.withType(CreateStaticLibrary::class)
  dependsOn(compiles, linkages, stripped, statics)

  inputs.dir(resources)

  if (HostManager.hostIsLinux) {
    inputs.dir(libs)

    listOf(
      "build/lib/main/release/shared",
      "build/lib/main/release/static",
    ).forEach {
      from(it) {
        exclude("**/stripped/**")
        into("META-INF/native/")
      }
    }
  }
}
