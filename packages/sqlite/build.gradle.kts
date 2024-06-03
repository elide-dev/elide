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
    id = "sqlite"
    name = "Elide SQLite3"
    description = "Packages native SQLite3 support for Elide."
  }
}

library {
  linkage = listOf(Linkage.STATIC, Linkage.SHARED)

  targetMachines = listOf(
    machines.windows.x86,
    machines.windows.x86_64,
    machines.macOS.x86_64,
    machines.macOS.architecture("arm64"),
    machines.linux.x86_64,
  )
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

val libMacros = mapOf(
  "SQLITE_ENABLE_LOAD_EXTENSION" to "1",
  "SQLITE_HAVE_ISNAN" to "1",
  "HAVE_USLEEP" to "1",
  "SQLITE_ENABLE_COLUMN_METADATA" to "1",
  "SQLITE_CORE" to "1",
  "SQLITE_ENABLE_FTS3" to "1",
  "SQLITE_ENABLE_FTS3_PARENTHESIS" to "1",
  "SQLITE_ENABLE_FTS5" to "1",
  "SQLITE_ENABLE_RTREE" to "1",
  "SQLITE_ENABLE_STAT4" to "1",
  "SQLITE_ENABLE_DBSTAT_VTAB" to "1",
  "SQLITE_ENABLE_MATH_FUNCTIONS" to "1",
  "SQLITE_THREADSAFE" to "1",
  "SQLITE_DEFAULT_MEMSTATUS" to "0",
  "SQLITE_DEFAULT_FILE_PERMISSIONS" to "0666",
  "SQLITE_MAX_VARIABLE_NUMBER" to "250000",
  "SQLITE_MAX_MMAP_SIZE" to "1099511627776",
  "SQLITE_MAX_LENGTH" to "2147483647",
  "SQLITE_MAX_COLUMN" to "32767",
  "SQLITE_MAX_SQL_LENGTH" to "1073741824",
  "SQLITE_MAX_FUNCTION_ARG" to "127",
  "SQLITE_MAX_ATTACHED" to "125",
  "SQLITE_MAX_PAGE_COUNT" to "4294967294",
  "SQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS" to "1",
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

val amalgamationRoot = rootProject.layout.projectDirectory.dir("third_party/sqlite")
val amalgamationSrcRoot = rootProject.layout.projectDirectory.dir("third_party/sqlite/src")

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

val buildAmalgamation by tasks.registering(Exec::class) {
  executable = "make"
  workingDir(rootProject.layout.projectDirectory)
  argumentProviders.add(CommandLineArgumentProvider {
    listOf("-C", "third_party", "sqlite")
  })
  outputs.upToDateWhen {
    layout.buildDirectory.file("sqlite3/sqlite3.c").get().asFile.exists()
  }
}

val checkBuilt by tasks.registering {
  group = "build"
  description = "Check if the SQLite3 amalgamation has been built"

  doLast {
    if (!amalgamationRoot.asFile.exists()) {
      throw IllegalStateException("SQLite3 not available. Please run `git submodule update --init --recursive`")
    }
  }

  outputs.upToDateWhen {
    amalgamationRoot.file("sqlite3.c").asFile.exists()
  }
  dependsOn(buildAmalgamation)
  mustRunAfter(buildAmalgamation)
}

val copyAmalgamation by tasks.registering(Copy::class) {
  group = "build"
  description = "Copy the SQLite3 amalgamation source"

  dependsOn(buildAmalgamation, checkBuilt)
  from(amalgamationRoot) {
    include("sqlite3.c", "sqlite3.h")
  }
  into(layout.buildDirectory.dir("sqlite3"))

  inputs.files(
    amalgamationRoot.file("sqlite3.c"),
    amalgamationRoot.file("sqlite3.h"),
  )
  outputs.files(
    layout.buildDirectory.file("sqlite3/sqlite3.c"),
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
  dependsOn(copyAmalgamation)
  mustRunAfter(checkBuilt)
  val layoutSources = layout.projectDirectory.dir("src/main/cpp").asFileTree.matching {
    include("**/*.c", "**/*.h")
  }
  val sqliteSources = layout.buildDirectory.dir("sqlite3").get().asFileTree.matching {
    include("**/*.c", "**/*.h")
  }

  macros.putAll(libMacros)
  source.from(layoutSources, sqliteSources)
  inputs.files(layoutSources, sqliteSources)

  inputs.files(
    layout.buildDirectory.file("sqlite3/sqlite3.c"),
    layout.buildDirectory.file("sqlite3/sqlite3.h"),
  )
  compilerArgs.addAll(listOf(
    "-x", "c",
    "-fPIC",
    "-I${amalgamationRoot.asFile.path}",
    "-I${amalgamationSrcRoot.asFile.path}",
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

tasks.processResources {
  val libs = layout.buildDirectory.dir("lib/main/release")
  val compiles = tasks.withType(CppCompile::class)
  val linkages = tasks.withType(LinkSharedLibrary::class)
  val stripped = tasks.withType(StripSymbols::class)
  val statics = tasks.withType(CreateStaticLibrary::class)
  dependsOn(compiles, linkages, stripped, statics)

  inputs.dir(libs)

  from("build/lib/main/release") {
    exclude("**/stripped/**")
    into("META-INF/native/")
  }
}
