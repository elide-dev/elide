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
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "graalvm-ts"
    name = "Elide TypeScript for GraalVM"
    description = "Integration package with GraalVM and TypeScript."

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
}

val enableEdgeJvm = false
val extraSrcroot = if (enableEdgeJvm) "src/main/java25x" else "src/main/java24x"

sourceSets {
  main {
    java.srcDirs(layout.projectDirectory.dir(extraSrcroot))
  }
}

dependencies {
  annotationProcessor(libs.graalvm.truffle.api)
  annotationProcessor(libs.graalvm.truffle.processor)
  api(projects.packages.engine)
  api(projects.packages.graalvmJs)
  api(libs.graalvm.truffle.api)
  implementation(libs.commons.io)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.graalvm.js.language)
  implementation(libs.graalvm.shadowed.icu4j)
  implementation(libs.graalvm.regex)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvm)
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

val elideTarget = TargetInfo.current(project)

val quickbuild = (
  properties["elide.release"] != "true" ||
    properties["elide.buildMode"] == "dev"
  )

val isRelease = !quickbuild && (
  properties["elide.release"] == "true" ||
    properties["elide.buildMode"] == "release"
  )

val nativesType = if (isRelease) "release" else "debug"

val umbrellaNativesPath: String =
  rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/$nativesType")
    .asFile
    .path
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

tasks {
  jar.configure {
    exclude("**/runtime.current.json")
  }

  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
    systemProperty("java.library.path", javaLibPath.get())
  }
}

val (jsGroup, jsName) = libs.graalvm.js.language.get()
  .let {
    it.group to it.name
  }
configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("$jsGroup:$jsName")).apply {
      using(project(":packages:graalvm-js"))
      because("Uses Elide's patched version of GraalJs")
    }
  }
}
