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
    id = "tcnative"
    name = "Elide Tomcat Native"
    description = "Packages native Tomcat support for Elide and Netty."
  }
}

library {
  linkage = listOf(Linkage.STATIC, Linkage.SHARED)

  when (val arch = System.getProperty("os.arch")) {
    "x86-64", "amd64" -> {
      targetMachines = listOf(
        when {
          HostManager.hostIsMingw -> machines.windows.x86_64
          HostManager.hostIsMac -> machines.macOS.x86_64
          HostManager.hostIsLinux -> machines.linux.x86_64
          else -> error("Unsupported OS for native builds on x86-64 (tcnative)")
        }
      )
    }

    "aarch64", "arm64" -> {
      targetMachines = listOf(
        when {
          HostManager.hostIsMingw -> machines.windows.architecture("arm64")
          HostManager.hostIsMac -> machines.macOS.architecture("arm64")
          HostManager.hostIsLinux -> machines.linux.architecture("arm64")
          else -> error("Unsupported OS for native builds on arm64 (tcnative)")
        }
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

val boringsslRoot = rootProject.layout.projectDirectory.dir("third_party/google/boringssl")
val boringsslBuildRoot = rootProject.layout.projectDirectory.dir("third_party/google/boringssl/build")
val boringsslHeaders = rootProject.layout.projectDirectory.dir("third_party/google/boringssl/include")

val libaprRoot = rootProject.layout.projectDirectory.dir("third_party/apache/apr")
val libaprBuildRoot = rootProject.layout.projectDirectory.dir("third_party/apache/apr/target/lib")
val libaprHeaders = rootProject.layout.projectDirectory.dir("third_party/apache/apr/target/include")

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

val buildBoringSsl by tasks.registering(Exec::class) {
  group = "build"
  description = "Build BoringSSL using CMake"
  executable = "make"
  workingDir(rootProject.layout.projectDirectory)
  argumentProviders.add(CommandLineArgumentProvider {
    listOf("-C", "third_party", "boringssl")
  })
  outputs.upToDateWhen {
    layout.buildDirectory.file("boringssl/libcrypto.a").get().asFile.exists()
  }
}

val testBoringSsl by tasks.registering(Exec::class) {
  group = "verify"
  description = "Run BoringSSL test suite"
  executable = "make"
  workingDir(boringsslBuildRoot)
  argumentProviders.add(CommandLineArgumentProvider {
    listOf("test")
  })
}

val buildLibApr by tasks.registering(Exec::class) {
  group = "build"
  description = "Build the Apache Portable Runtime libraries"
  executable = "make"
  workingDir(rootProject.layout.projectDirectory)
  argumentProviders.add(CommandLineArgumentProvider {
    listOf("-C", "third_party", "apr")
  })
  outputs.upToDateWhen {
    layout.buildDirectory.file("libapr/libapr.a").get().asFile.exists()
  }
}

val testLibApr by tasks.registering(Exec::class) {
  group = "verify"
  description = "Run APR test suite"
  executable = "make"
  workingDir(libaprRoot)
  argumentProviders.add(CommandLineArgumentProvider {
    listOf("test")
  })
}

val checkBuilt by tasks.registering {
  group = "build"
  description = "Check if the BoringSSL and APR libs have been built"

  doLast {
    if (!boringsslRoot.asFile.exists()) {
      throw IllegalStateException("BoringSSL not available. Please run `git submodule update --init --recursive`")
    }
    if (!libaprRoot.asFile.exists()) {
      throw IllegalStateException("APR not available. Please run `git submodule update --init --recursive`")
    }
  }

  outputs.upToDateWhen {
    (
      layout.buildDirectory.file("boringssl/libcrypto.a").get().asFile.exists() &&
      layout.buildDirectory.file("libapr/libapr.a").get().asFile.exists()
    )
  }
  dependsOn(buildBoringSsl, buildLibApr)
  mustRunAfter(buildBoringSsl, buildLibApr)
}

val copyLibAprLibrary by tasks.registering(Copy::class) {
  group = "build"
  description = "Copy APR libraries"

  dependsOn(buildLibApr, checkBuilt)
    val libTail = if (HostManager.hostIsMac) "dylib" else "so"

  from(libaprBuildRoot) {
    include("libapr-2.0.a", "libapr-2.a", "libapr-2.so.0.0.0", "libapr-2.0.dylib")
    rename {
      (if (it == "libapr-2.so.0.0.0") "libapr-2.$libTail" else it).replace("-2.0.", "-2.")
    }
  }
  from(libaprHeaders) {
    include("**/*.h")
  }
  into(layout.buildDirectory.dir("libapr"))

  inputs.files(
    libaprBuildRoot.file("libapr-2.a"),
    libaprBuildRoot.file("libapr-2.0.$libTail"),
  )
  outputs.files(
    layout.buildDirectory.file("libapr/libapr-2.a"),
    layout.buildDirectory.file("libapr/libapr-2.0.$libTail"),
  )
}

val copyBoringSslLibrary by tasks.registering(Copy::class) {
  group = "build"
  description = "Copy the BoringSSL built library"

  dependsOn(buildBoringSsl, checkBuilt)

  from(boringsslBuildRoot) {
    include("libssl.a", "libcrypto.a", "libdecrepit.a")
  }
  from(boringsslHeaders) {
    include("**/*.h")
    exclude("openssl/pki")
    exclude("openssl/experimental")
  }
  into(layout.buildDirectory.dir("boringssl"))

  inputs.files(
    boringsslBuildRoot.file("libssl.a"),
    boringsslBuildRoot.file("libcrypto.a"),
    boringsslBuildRoot.file("libdecrepit.a"),
    boringsslHeaders.file("openssl/bio.h"),
  )
  outputs.files(
    layout.buildDirectory.file("boringssl/libssl.a"),
    layout.buildDirectory.file("boringssl/libcrypto.a"),
    layout.buildDirectory.file("boringssl/libdecrepit.a"),
    layout.buildDirectory.file("boringssl/openssl/bio.h"),
  )
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
  dependsOn(copyBoringSslLibrary, copyLibAprLibrary)
  mustRunAfter(checkBuilt)

  val layoutSources = layout.projectDirectory.dir("src/main/cpp").asFileTree.matching {
    include("**/*.c", "**/*.h")
  }
  val boringsslBuildPath = layout.buildDirectory.dir("boringssl").get()
  val boringsslLibs = boringsslBuildPath.asFileTree.matching {
    include("**/*.a")
  }
  val boringsslHeaders = boringsslBuildPath.asFileTree.matching {
    include("**/*.h")
  }
  val libaprBuildPath = layout.buildDirectory.dir("libapr").get()
  val libaprLibs = libaprBuildPath.asFileTree.matching {
    include("**/*.a")
  }
  val libaprHeaders = libaprBuildPath.asFileTree.matching {
    include("**/*.h")
  }

  macros.putAll(libMacros)
  source.from(layoutSources, boringsslHeaders, libaprHeaders)
  inputs.files(layoutSources, boringsslLibs, libaprLibs)

  // enable static init mode
  if (name.lowercase().contains("static")) {
    macros["ELIDE_GVM_STATIC"] = "1"
    macros["TCN_BUILD_STATIC"] = "1"
  }

  inputs.files(
    layout.buildDirectory.file("boringssl/libssl.a"),
    layout.buildDirectory.file("boringssl/libcrypto.a"),
    layout.buildDirectory.file("boringssl/libdecrepit.a"),
  )
  compilerArgs.addAll(listOf(
    "-x", "c",
    "-fPIC",
    "-I$jdkIncludePath",
    "-I$jdkNativeIncludePath",
    "-I${boringsslBuildPath.asFile.path}",
    "-I${libaprBuildPath.asFile.path}",
    "-I${libaprBuildPath.asFile.path}/apr-2",
  ).plus(if (!HostManager.hostIsLinux) emptyList() else listOf(
    "-I/usr/include",
  )))
}

tasks.withType(LinkSharedLibrary::class.java).configureEach {
  group = "build"
  description = "Link shared libraries"

  val boringsslBuildPath = layout.buildDirectory.dir("boringssl").get()
  val libaprBuildPath = layout.buildDirectory.dir("libapr").get()

  linkerArgs.addAll(listOf(
    "-L$jdkLibPath",
    "-L${boringsslBuildPath.asFile.path}",
    "-L${libaprBuildPath.asFile.path}",
    "-lcrypto",
    "-lssl",
    "-lapr-2",
  ).plus(if (!HostManager.hostIsLinux) listOf(
    "-liconv",
  ) else listOf(
    "-L/usr/lib",
  )))
}

val compile by tasks.registering {
  dependsOn(tasks.withType(CppCompile::class))
}

val link by tasks.registering {
  dependsOn(tasks.withType(LinkSharedLibrary::class))
}

tasks.build {
  dependsOn(buildBoringSsl, buildLibApr, compile, link)
}

tasks.processResources {
  val libs = layout.buildDirectory.dir("lib/main/release")
  val bsslLibs = layout.buildDirectory.dir("boringssl")
  val aprLibs = layout.buildDirectory.dir("libapr")
  val compiles = tasks.withType(CppCompile::class)
  val linkages = tasks.withType(LinkSharedLibrary::class)
  val stripped = tasks.withType(StripSymbols::class)
  val statics = tasks.withType(CreateStaticLibrary::class)
  dependsOn(compiles, linkages, stripped, statics)

  inputs.dir(libs)
  inputs.dir(bsslLibs)
  inputs.dir(aprLibs)

  val arch = System.getProperty("os.arch")
    .replace("-", "_")
  val osName = System.getProperty("os.name")
    .replace("Mac OS X", "macos")
    .replace(" ", "-")
    .lowercase()

  from(bsslLibs) {
    exclude("**/stripped/**")
    exclude("**/*.h")
    into("META-INF/native/$osName/$arch/")
  }
  from(aprLibs) {
    exclude("**/stripped/**")
    exclude("**/*.h")
    into("META-INF/native/$osName/$arch/")
  }

  from("build/lib/main/release") {
    exclude("**/stripped/**")
    into("META-INF/native/$osName/$arch/")
  }
}