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

@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import me.champeau.jmh.JMHTask
import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import elide.toolchain.host.TargetInfo

plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

val javaLanguageVersion = "23"

sourceSets.all {
  java.setSrcDirs(listOf("jmh/src"))
  resources.setSrcDirs(listOf("jmh/resources"))
}

allOpen {
  annotation("org.openjdk.jmh.annotations.BenchmarkMode")
}

dependencies {
  implementation(libs.kotlinx.benchmark.runtime)
  implementation(libs.kotlinx.coroutines.test)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.inject.java.test)
  implementation(mn.micronaut.runtime)
  implementation(projects.packages.graalvm)
  implementation(projects.packages.cli)
  implementation(project(":packages:cli", configuration = "cliNativeOptimized"))
  implementation(libs.lmax.disruptor.core)
  implementation(libs.lmax.disruptor.proxy)
  compileOnly(libs.graalvm.svm)
  runtimeOnly(libs.logback)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
  configurations {
    named("main") {
      warmups = 5
      iterations = 5
    }
    create("sqlite") {
      include("*SQLite*")
      warmups = 5
      iterations = 5
    }
    create("entry") {
      include("EntryBenchmark")
      warmups = 5
      iterations = 5
    }
    create("node") {
      include("Node*")
      warmups = 5
      iterations = 5
    }
    create("context") {
      include(".*Context.*")
      warmups = 5
      iterations = 5
    }
  }
  targets {
    register("main") {
      this as JvmBenchmarkTarget
      jmhVersion = libs.versions.jmh.lib.get()
    }
  }
}

val elideTarget = TargetInfo.current(project)
val nativesType = "release"
val umbrellaNativesPath: String =
  rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/$nativesType").asFile.path
val nativesPath = umbrellaNativesPath
val targetSqliteDir = rootProject.layout.projectDirectory.dir("third_party/sqlite/install")
val targetSqliteLibDir = targetSqliteDir.dir("lib")

val javaLibPath = provider {
  StringBuilder().apply {
    append(nativesPath)
    append(File.pathSeparator)
    append(targetSqliteLibDir)
    System.getProperty("java.library.path", "").let {
      if (it.isNotEmpty()) {
        append(File.pathSeparator)
        append(it)
      }
    }
  }
}

tasks.withType(JMHTask::class).configureEach {
  doNotTrackState("always run")
  outputs.upToDateWhen { false }
  jvmArgsAppend.addAll(
    listOf(
      "-XX:+UnlockExperimentalVMOptions",
      "-Djava.util.concurrent.ForkJoinPool.common.parallelism=1",
      "-Djava.library.path=${javaLibPath.get()}",
    ),
  )
}

tasks.withType(Jar::class).configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType(Copy::class).configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(javaLanguageVersion)
    javaParameters = true
    incremental = true
    freeCompilerArgs.add("-Xskip-prerelease-check")
  }
}

fun checkNatives() {
  if (!File(nativesPath).exists()) {
    error("Natives not found at $nativesPath; please run `./gradlew natives -Pelide.release=true`")
  }
}

val projectRootPath: String = rootProject.layout.projectDirectory.asFile.absolutePath

tasks.withType<JavaExec>().configureEach {
  doFirst {
    checkNatives()
  }
  jvmArgs(
    listOf(
      "--enable-preview",
      "--enable-native-access=ALL-UNNAMED",
      "-XX:+UseG1GC",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+TrustFinalNonStaticFields",
      "-Djava.library.path=${javaLibPath.get()}",
      "-Delide.disableStreams=true",
      "-Delide.project.root=$projectRootPath",
    ),
  )
}
