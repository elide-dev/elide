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
    id = "terminal"
    name = "Elide Terminal Native"
    description = "Packages native terminal/console libraries like Jansi for use with Elide."
  }
}

library {
  linkage = listOf(Linkage.STATIC, Linkage.SHARED)

  when (val arch = System.getProperty("os.arch")) {
    "x86-64", "amd64" -> {
      targetMachines = listOf(
        machines.windows.x86,
        machines.macOS.x86_64,
        machines.linux.x86_64,
      )
    }

    "aarch64", "arm64" -> {
      targetMachines = listOf(
        machines.macOS.architecture("arm64"),
      )
    }

    else -> error("Unfamiliar architecture: $arch")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

val libMacros = mapOf(
  "ELIDE" to "1",
)

val jvmApi: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
val apiElements: Configuration by configurations.getting { extendsFrom(jvmApi) }
val compileClasspath: Configuration by configurations.getting { extendsFrom(jvmApi) }

dependencies {
  jvmApi(libs.slf4j)
  compileOnly(libs.graalvm.svm)
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

tasks.compileJava {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    listOf(
      "-nowarn",
      "-Xlint:none",
    )
  })
}

tasks.withType(CppCompile::class) {
  group = "build"
  description = "Compile C/C++ sources"

  val layoutSources = layout.projectDirectory.dir("src/main/cpp").asFileTree.matching {
    include("**/*.c", "**/*.h")
  }
  macros.putAll(libMacros)
  source.from(layoutSources)
  inputs.files(layoutSources)

  // enable static init mode
  if (name.lowercase().contains("static")) {
    macros["ELIDE_GVM_STATIC"] = "1"
  }

  compilerArgs.addAll(listOf(
    "-x", "c",
    "-fPIC",
    "-I$jdkIncludePath",
    "-I$jdkNativeIncludePath",
  ))
}

tasks.withType(LinkSharedLibrary::class.java).configureEach {
  group = "build"
  description = "Link shared libraries"

  linkerArgs.addAll(listOf(
    "-L$jdkLibPath",
  ))
}

val compile by tasks.registering {
  dependsOn(tasks.withType(CppCompile::class))
}

val link by tasks.registering {
  dependsOn(tasks.withType(LinkSharedLibrary::class))
}

tasks.build {
  dependsOn(compile, link)
}

tasks.processResources {
  val libs = layout.buildDirectory.dir("lib/main/release")
  val compiles = tasks.withType(CppCompile::class)
  val linkages = tasks.withType(LinkSharedLibrary::class)
  val stripped = tasks.withType(StripSymbols::class)
  val statics = tasks.withType(CreateStaticLibrary::class)
  dependsOn(compiles, linkages, stripped, statics)

  inputs.dir(libs)

  val arch = System.getProperty("os.arch")
    .replace("-", "_")
    .replace("x86_64", "amd64")
  val osName = System.getProperty("os.name")
    .replace("Mac OS X", "macos")
    .replace(" ", "-")
    .lowercase()

  from("build/lib/main/release") {
    exclude("**/stripped/**")
    into("META-INF/native/$osName/$arch/")
    eachFile {
      val libtype = if (path.contains("static")) "static" else "shared"
      path = "META-INF/native/$osName/$arch/${libtype}/${path.substringAfterLast('/')}"
    }
  }
}
