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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import elide.internal.conventions.publishing.publish
import elide.toolchain.host.TargetInfo

plugins {
  `java-library`
  alias(libs.plugins.elide.conventions)
}

val currentPlatform = StringBuilder().apply {
  append(System.getProperty("os.name").lowercase().replace("mac os x", "macos"))
  append("-")
  append(System.getProperty("os.arch").lowercase())
}.toString()

elide {
  checks {
    spotless = false
    checkstyle = false
    detekt = false
  }

  publishing {
    id = "sqlite"
    name = "Elide SQLite"
    description = "Supporting code for SQLite use within the context of Elide guest VMs"

    publish("sqlite") {
      from(components["java"])
    }
  }

  jvm {
    alignVersions = true
  }
}

dependencies {
  api(libs.slf4j)
  compileOnly(libs.graalvm.svm)
}

val buildModeStr = (findProperty("elide.buildMode") as? String)?.ifBlank { null } ?: "debug"
 val buildMode = when (buildModeStr) {
   "dev", "debug" -> "debug"
   "release" -> "release"
   else -> error("Unsupported build mode: '$buildModeStr'")
 }
// @TODO: build mode fixes
//val buildMode = "debug"
val libPostfix = when {
  HostManager.hostIsMac -> "dylib"
  HostManager.hostIsLinux -> "so"
  HostManager.hostIsMingw -> "dll"
  else -> error("Unsupported host platform")
}
val libPrefix = when {
  HostManager.hostIsMac -> "lib"
  HostManager.hostIsLinux -> "lib"
  HostManager.hostIsMingw -> ""
  else -> error("Unsupported host platform")
}
val sqliteJdbcLibName = StringBuilder().apply {
  append(libPrefix)
  append("sqlite")
  append("jdbc")
  append(".")
  append(libPostfix)
}
val platformLibFileName = StringBuilder().apply {
  append(libPrefix)
  append("sqlite")
  append("jdbc")
  append("-")
  append(currentPlatform)
  append(".")
  append(libPostfix)
}.toString()

val elideTarget = TargetInfo.current(project)

val sqliteJdbcLib: String = rootProject
  .layout
  .projectDirectory
  .file("target/${elideTarget.triple}/$buildMode/$sqliteJdbcLibName")
  .asFile
  .path

val libsDir: String =
  layout.buildDirectory.dir("native-libs").get().asFile.path

val libFileName: String =
  layout.buildDirectory.file("native-libs/$platformLibFileName").get().asFile.path

val copyNative by tasks.registering(Copy::class) {
  dependsOn(
    ":packages:graalvm:natives",
  )

  from(sqliteJdbcLib)
  into(libsDir)
  inputs.file(sqliteJdbcLib)
  outputs.file(libFileName)
  dependsOn(":packages:graalvm:buildRustNativesForHost")
  dependsOn(":packages:graalvm:buildRustNativesForHostDebug")
}

val nativeResources: String =
  layout.buildDirectory.dir("resources/main/META-INF/native/${elideTarget.triple}").get().asFile.path

val finalLibLocation: String =
  layout.buildDirectory.file("resources/main/META-INF/native/$platformLibFileName").get().asFile.path

val mountNative by tasks.registering(Copy::class) {
  dependsOn(copyNative.name)
  from(libsDir)
  into(nativeResources)
  outputs.file(finalLibLocation)
}

tasks.compileJava {
  options.compilerArgs = options.compilerArgs ?: mutableListOf()
  options.compilerArgs.addAll(listOf(
    "-nowarn",
    "-Xlint:none",
  ))
}

tasks.processResources {
  dependsOn(mountNative)
}

tasks.jar {
  archiveClassifier = currentPlatform
}
