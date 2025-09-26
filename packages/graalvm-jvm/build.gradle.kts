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

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")

val patchedLibs = files(
  gvmJarsRoot.file("espresso.jar"),
  gvmJarsRoot.file("espresso-shared.jar"),
  gvmJarsRoot.file("truffle-api.jar"),
)

val patchedDependencies: Configuration by configurations.creating { isCanBeResolved = true }

elide {
  publishing {
    id = "graalvm-jvm"
    name = "Elide JVM integration package for GraalVM"
    description = "Integration package with GraalVM, Elide, and JVM."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }

  java {
    // disable module-info processing (not present)
    configureModularity = false
  }

  native {
    target = NativeTarget.LIB
  }
}

val enablePackagedJvm = false
val enableGraalEmbedded = false

val packagedModules = listOf(
  "java.base",
  "java.datatransfer",
  "java.desktop",
  "java.instrument",
  "java.logging",
  "java.management",
  "java.naming",
  "java.net.http",
  "java.prefs",
  "java.sql",
  "java.sql.rowset",
  "java.transaction.xa",
  "java.xml",
  "java.xml.crypto",
  "jdk.accessibility",
  "jdk.attach",
  "jdk.charsets",
  "jdk.crypto.cryptoki",
  "jdk.graal.compiler",
  "jdk.unsupported",
  "jdk.hotspot.agent",
  "jdk.httpserver",
  "jdk.incubator.vector",
  "jdk.localedata",
  "jdk.naming.dns",
  "jdk.net",
  "jdk.nio.mapmode",
  "jdk.zipfs",
).plus(
  if (enableGraalEmbedded) {
    listOf(
      "org.graalvm.collections",
      "org.graalvm.nativeimage",
      "org.graalvm.nativeimage.base",
      "org.graalvm.nativeimage.builder",
      "org.graalvm.nativeimage.foreign",
      "org.graalvm.nativeimage.objectfile",
      "org.graalvm.nativeimage.pointsto",
      "org.graalvm.truffle.compiler",
      "org.graalvm.word",
    )
  } else {
    emptyList()
  }
)

val limitedModules = listOf<String>()

val packagedJvmArgs = listOf(
  "-Delide.jvm.bundled=true",
)

val jdkModule: SourceSet by sourceSets.creating {
  java.srcDirs(layout.projectDirectory.dir("src/main/jdk"))
}

val jlink: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

val jdkHome = (
  System.getenv("JLINK_HOME")
    ?: System.getenv("JAVA_HOME")
    ?: System.getenv("GRAALVM_HOME")
    ?: System.getProperty("java.home")
  )?.ifBlank { null } ?: error(
  "Please set GRAALVM_HOME or JAVA_HOME for use of jlink."
)

val jlinkPath: String = File(jdkHome).resolve("bin").resolve("jlink").absolutePath
val jmodPath: String = File(jdkHome).resolve("bin").resolve("jmod").absolutePath
val jlinkOut: String = layout.buildDirectory.dir("jlink")
  .get()
  .asFile
  .absolutePath
val optsOut: String = layout.buildDirectory.file("jlink.args")
  .get()
  .asFile
  .absolutePath

dependencies {
  api(projects.packages.engine)
  api(libs.graalvm.polyglot.java.community)
  implementation(libs.classgraph)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.graalvm.espresso.hotswap)
  implementation(libs.graalvm.truffle.nfi.libffi)
  implementation(libs.graalvm.espresso.polyglot)
  implementation(libs.graalvm.espresso.resources.jdk21)

  // patched for use of host source loader
  // implementation(libs.graalvm.espresso.language)

  api(patchedLibs)
  patchedDependencies(patchedLibs)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvm)
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

val builderLibsPath: String = File(jdkHome)
  .resolve("lib")
  .resolve("svm")
  .resolve("builder")
  .absolutePath

val classesOut = layout.buildDirectory.dir("jdk-classes")
val jmodDir = layout.buildDirectory.dir("jmod")
val jmodOut: String = jmodDir.get()
  .asFile
  .resolve("elide-jdk.jmod")
  .absolutePath

val compileJdkModule by tasks.registering(JavaCompile::class) {
  modularity.inferModulePath = true
  source(jdkModule.java)
  classpath = jlink
  destinationDirectory = classesOut
}

val jmodExists = File(jmodOut).exists()
val compileJmod by tasks.registering(Exec::class) {
  enabled = enablePackagedJvm && !jmodExists
  executable = jmodPath
  dependsOn(compileJdkModule.name)
  outputs.file(jmodOut)

  args(buildList {
    add("create")
    add("--compress=zip-9")
    add("--class-path=${classesOut.get().asFile.absolutePath}:")
    add(jmodOut)
  })
}

val jlinkBinRoot = File(jlinkOut).resolve("bin")
val jlinkConfRoot = File(jlinkOut).resolve("conf")

val binsToRemove = listOf(
  // These are shipped via Elide's own CLI.
  jlinkBinRoot.resolve("javac"),
  jlinkBinRoot.resolve("jar"),
  jlinkBinRoot.resolve("javadoc"),
  jlinkBinRoot.resolve("jimage"),
  jlinkBinRoot.resolve("jlink"),
  jlinkBinRoot.resolve("jmod"),
  jlinkBinRoot.resolve("jpackage"),
  jlinkBinRoot.resolve("jrunscript"),
  jlinkBinRoot.resolve("jshell"),
  jlinkBinRoot.resolve("jstatd"),
  jlinkBinRoot.resolve("jwebserver"),
  jlinkBinRoot.resolve("rmiregistry"),
)

val resourcesToRemove = listOf(
  jlinkConfRoot.resolve("sound.properties"),
)

val trimPackagedJvm by tasks.registering(Exec::class) {
  executable = "rm"
  args(listOf(
    "-f",
  ).plus(
    binsToRemove.map { it.absolutePath }
  ).plus(
    resourcesToRemove.map { it.absolutePath }
  ))
}

val jdkBinRoot: String = File(jdkHome)
  .resolve("bin")
  .absolutePath

val copyNativeImageBuilder by tasks.registering(Copy::class) {
  from(jdkBinRoot) {
    include("native-image")
    include("native-image-configure")
    include("native-image-inspect")
  }
  into(jlinkBinRoot.absolutePath)
  doNotTrackState("copying to directory")
}

val jlinkExists = File(jlinkOut).exists()
val jmodDirPath: String = jmodDir.get().asFile.path

val buildPackagedJvm by tasks.registering(Exec::class) {
  enabled = enablePackagedJvm && !jlinkExists
  executable = jlinkPath
  dependsOn(compileJmod.name)
  finalizedBy(copyNativeImageBuilder.name)
  finalizedBy(trimPackagedJvm.name)

  args(buildList {
    add("--module-path=.$jmodDirPath:$builderLibsPath")
    add("--bind-services")
    add("--compress=zip-9")
    add("--no-header-files")
    add("--no-man-pages")
    add("--strip-debug")
    add("--save-opts=$optsOut")
    add("--output=$jlinkOut")
    add("--dedup-legal-notices=error-if-not-same-content")
    add("--exclude-jmod-section=man")
    add("--exclude-jmod-section=headers")
    add("--generate-cds-archive")
    add("--vendor-bug-url=https://github.com/elide-dev/elide")
    add("--vendor-vm-bug-url=https://github.com/elide-dev/elide")
    add("--vendor-version=${version as String}")
    add("--vm=server")
    add("--include-locales=en")

    packagedModules.forEach {
      add("--add-modules=$it")
    }
    limitedModules.forEach {
      add("--limit-modules=$it")
    }
    packagedJvmArgs.forEach {
      add("--add-options=$it")
    }
  })
}

val jvmBundle by tasks.registering {
  group = "build"
  description = "Build the embedded JVM bundle shipped with Elide"
  dependsOn(buildPackagedJvm.name)
}

tasks.build {
  if (enablePackagedJvm) {
    dependsOn(jvmBundle.name)
  }
}

tasks.test {
  jvmArgs.add("--enable-native=ALL-UNNAMED")
}
