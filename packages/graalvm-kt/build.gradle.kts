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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.io.path.absolutePathString
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "graalvm-kt"
    name = "Elide Kotlin for GraalVM"
    description = "Integration package with GraalVM, Espresso, and the Kotlin compiler."

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

  checks {
    diktat = false
  }
}

val javacFlags = listOf(
  "--add-exports=java.base/jdk.internal.jrtfs=ALL-UNNAMED",
)

fun Configuration.embeddedConfiguration() {
  isCanBeConsumed = true

  resolutionStrategy {
    failOnDynamicVersions()
    failOnNonReproducibleResolution()
  }
}

// exported to other projects
val embeddedKotlin: Configuration by configurations.creating { embeddedConfiguration() }
val embeddedJava: Configuration by configurations.creating { embeddedConfiguration() }

val embeddedKotlinRuntime = layout.projectDirectory.file(
  "src/main/resources/META-INF/elide/embedded/runtime/kt/elide-kotlin-runtime.jar"
).asFile

dependencies {
  api(projects.packages.engine)
  api(libs.graalvm.truffle.api)
  api(libs.graalvm.espresso.polyglot)
  api(libs.graalvm.espresso.language)
  annotationProcessor(libs.graalvm.truffle.processor)
  implementation(projects.packages.graalvmJvm)

  implementation(libs.kotlinx.atomicfu)
  implementation(libs.kotlin.scripting.jvm)
  implementation(libs.kotlin.scripting.jvm.host)
  implementation(libs.kotlin.scripting.compiler.embeddable)
  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.kotlin.scripting.dependencies)
  implementation(libs.kotlin.scripting.dependencies.maven)
  api(files(embeddedKotlinRuntime))
  compileOnly(libs.graalvm.svm)

  embeddedKotlin(libs.kotlin.stdlib)
  embeddedKotlin(libs.kotlin.reflect)
  embeddedKotlin(libs.kotlin.test.junit5)
  embeddedKotlin(libs.kotlin.scripting.runtime)
  embeddedKotlin(libs.kotlin.scripting.jvm)
  embeddedKotlin(libs.kotlinx.atomicfu.jvm)
  embeddedKotlin(libs.kotlinx.io.jvm)
  embeddedKotlin(libs.kotlinx.io.bytestring.jvm)
  embeddedKotlin(libs.kotlinx.coroutines.core.jvm)
  embeddedKotlin(libs.kotlinx.coroutines.jdk9)
  embeddedKotlin(libs.kotlinx.coroutines.slf4j)
  embeddedKotlin(libs.kotlinx.serialization.core.jvm)
  embeddedKotlin(libs.kotlinx.serialization.json.jvm)
  embeddedKotlin(libs.kotlinx.html.jvm)
  embeddedKotlin(libs.kotlinx.wrappers.css.jvm)
  embeddedKotlin(libs.ksp)
  embeddedKotlin(libs.ksp.api)
  embeddedKotlin(libs.ksp.cmdline)
  embeddedKotlin(mn.micronaut.inject.kotlin)
  embeddedKotlin(files(embeddedKotlinRuntime))

  embeddedJava(libs.jacoco.agent)
  embeddedJava(libs.junit.jupiter.api)
  embeddedJava(libs.junit.jupiter.params)
  embeddedJava(libs.junit.jupiter.engine)
  embeddedJava(libs.junit.platform.commons)
  embeddedJava(libs.junit.platform.console)
  embeddedJava(libs.slf4j)
  embeddedJava(mn.micronaut.inject.java)
  embeddedJava(mn.micronaut.test.junit5)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvm)
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

val stdlibJar = provider {
  embeddedKotlin.filter {
    it.name.contains("kotlin") && it.name.contains("stdlib")
  }
}
val reflectJar = provider {
  embeddedKotlin.filter {
    it.name.contains("kotlin") && it.name.contains("reflect")
  }
}
val scriptRuntimeJar = provider {
  embeddedKotlin.filter {
    it.name.contains("kotlin") && it.name.contains("script-runtime")
  }
}

val filesMap = mapOf(
  "kotlin-stdlib.jar" to stdlibJar,
  "kotlin-reflect.jar" to reflectJar,
  "kotlin-script-runtime.jar" to scriptRuntimeJar,
)

val jvmDefs = listOf(
  "elide.kotlin.version" to libs.versions.kotlin.sdk.get(),
  "elide.kotlin.verbose" to "false",
)

val defs = provider {
  jvmDefs.plus(filesMap.map {
    (it.key to it.value.get().singleFile).also { (_, file) ->
      require(file.exists()) { "File $file does not exist" }
    }
  }.map {
    val key = it.first
      .replace("kotlin-", "")
      .replace(".jar", "")
      .replace("_", "-")

    "elide.kotlin.$key" to it.second.absolutePath
  }).map {
    "-D${it.first}=${it.second}"
  }
}

val archiveName = "kotlin-resources.zip"
val kotlinHomeRoot = layout.buildDirectory.dir("kotlin-resources/kotlin")
val resourcesManifest = kotlinHomeRoot.map { it.file("embedded-classpath.txt") }
val intermediateResources = kotlinHomeRoot.map { it.dir(libs.versions.kotlin.sdk.get()) }
val intermediateResourcesZip = layout.buildDirectory.file(archiveName)

val prepKotlinResources by tasks.registering(Copy::class) {
  from(embeddedKotlin + embeddedJava) {
    rename {
      // remove version tag from each jar
      val name = it.split(".").first().split("-")
        .dropLast(1).joinToString("-")
      if (name.isEmpty()) {
        it
      } else {
        "$name.jar"
      }
    }
  }
  destinationDir = intermediateResources.get().dir("lib").asFile
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val ktRuntimeTarget = "META-INF/elide/embedded/runtime/kt"
val ktRuntimeRoot = layout.projectDirectory.dir("src/main/resources/$ktRuntimeTarget")
val buildKotlinResourcesArchive by tasks.registering(Zip::class) {
  dependsOn(prepKotlinResources)
  archiveFileName = archiveName
  destinationDirectory = intermediateResourcesZip.get().asFile.parentFile
  from(intermediateResources.get().asFile)
}

val buildResourcesManifest by tasks.registering {
  inputs.files(embeddedJava + embeddedKotlin)
  outputs.file(resourcesManifest)
  doNotTrackState("too small to be worthy of caching")

  val renderedManifest = StringBuilder().apply {
    listOf(embeddedJava, embeddedKotlin).flatMap {
      it.resolvedConfiguration.resolvedArtifacts
    }.forEach { artifact ->
      val name = artifact.file.name.split(".").first().split("-")
        .dropLast(1).joinToString("-")
      val renamed = if (name.isEmpty()) {
        artifact.file.name
      } else {
        "$name.jar"
      }
      append(artifact.moduleVersion.id.group)
      append(':')
      append(artifact.moduleVersion.id.name)
      append(':')
      append(artifact.moduleVersion.id.version)
      artifact.classifier?.ifEmpty { null }?.let {
        append(':')
        append(artifact.classifier)
      }
      append('/')
      append(renamed)
      append('\n')
    }
  }

  val manifestPath = resourcesManifest.get().asFile.toPath().absolutePathString()

  actions.add {
    File(manifestPath).writeText(
      renderedManifest.toString()
    )
  }
}

tasks.processResources {
  dependsOn(prepKotlinResources, buildKotlinResourcesArchive, buildResourcesManifest)

  from(intermediateResources.get().dir("lib")) {
    into(ktRuntimeTarget)
  }
}

artifacts {
  add(embeddedKotlin.name, prepKotlinResources) {
    builtBy(prepKotlinResources)
  }
}

val kotlinHomePath: String = kotlinHomeRoot.get().asFile.absolutePath
val testJvmArgs = defs.get()

tasks.test {
  dependsOn(
    prepKotlinResources.name,
    buildKotlinResourcesArchive.name,
  )
  environment(
    "KOTLIN_HOME" to kotlinHomePath,
    "ELIDE_KOTLIN_HOME" to ktRuntimeRoot.asFile.absolutePath,
  )
  requireNotNull(jvmArgs).addAll(testJvmArgs)
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.addAll(javacFlags.map { "-Xjavac-arguments=$it" })
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(javacFlags)
}
