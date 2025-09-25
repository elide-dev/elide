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
import java.security.MessageDigest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.absolutePathString
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish
import java.util.Base64
import java.util.TreeSet

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
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

fun Configuration.embeddedConfiguration(kotlin: Boolean = false) {
  isCanBeConsumed = true

  resolutionStrategy {
    failOnDynamicVersions()
    failOnNonReproducibleResolution()
  }

  if (kotlin) {
    listOf(
      "org.jetbrains.kotlin" to "kotlin-gradle-plugin-api",
      "org.jetbrains.kotlin" to "kotlin-gradle-plugin-model",
    ).forEach {
      exclude(group = it.first, module = it.second)
    }
  }

  attributes {
    if (kotlin) {
      attributes.attribute(
        org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute,
        org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
      )
    }
  }
}

// exported to other projects
val embeddedKotlin: Configuration by configurations.creating { embeddedConfiguration(kotlin = true) }
val embeddedJava: Configuration by configurations.creating { embeddedConfiguration() }

val embeddedKotlinRuntime = layout.projectDirectory.file(
  "src/main/resources/META-INF/elide/embedded/runtime/kt/elide-kotlin-runtime.jar"
).asFile

val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")

val patchedLibs = files(
  gvmJarsRoot.file("espresso.jar"),
  gvmJarsRoot.file("espresso-shared.jar"),
  gvmJarsRoot.file("truffle-api.jar"),
  gvmJarsRoot.file("truffle-coverage.jar"),
  gvmJarsRoot.file("library-support.jar"),
  gvmJarsRoot.file("svm-driver.jar"),
)

val patchedDependencies: Configuration by configurations.creating { isCanBeResolved = true }

dependencies {
  api(projects.packages.engine)
  api(libs.graalvm.truffle.api)
  api(libs.graalvm.espresso.polyglot)
  annotationProcessor(libs.graalvm.truffle.processor)
  implementation(projects.packages.graalvmJvm)

  // note: patched for use of host-source-loader
  // api(libs.graalvm.espresso.language)
  patchedDependencies(patchedLibs)

  implementation(libs.kotlinx.atomicfu)
  implementation(libs.kotlin.scripting.jvm)
  implementation(libs.kotlin.scripting.jvm.host)
  implementation(libs.kotlin.scripting.compiler.embeddable)
  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.kotlin.serialization.embedded)
  implementation(libs.kotlin.powerAssert.embedded)
  implementation(libs.plugin.redacted.core)
  implementation(libs.kotlin.scripting.dependencies)
  implementation(libs.kotlin.scripting.dependencies.maven) {
    exclude(group = "com.google.inject", module = "guice")
  }
  implementation(libs.kotlinx.serialization.json.jvm)
  api(files(embeddedKotlinRuntime))
  compileOnly(libs.graalvm.svm)

  // elide modules
  embeddedKotlin(projects.packages.base) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
  }
  embeddedKotlin(projects.packages.core) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
  }
  embeddedKotlin(projects.packages.test) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
  }

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
  embeddedKotlin(libs.kotlinx.coroutines.test)
  embeddedKotlin(libs.kotlinx.serialization.core.jvm)
  embeddedKotlin(libs.kotlinx.serialization.json.jvm)
  embeddedKotlin(libs.kotlinx.html.jvm)
  embeddedKotlin(libs.kotlinx.wrappers.css.jvm)
  embeddedKotlin(libs.ksp)
  embeddedKotlin(libs.ksp.api)
  embeddedKotlin(libs.ksp.cmdline)
  embeddedKotlin(libs.plugin.redacted.core)
  embeddedKotlin(mn.micronaut.inject.kotlin)
  embeddedKotlin(libs.junit.jupiter.api)
  embeddedKotlin(libs.junit.jupiter.params)
  embeddedKotlin(libs.junit.jupiter.engine)
  embeddedKotlin(libs.junit.platform.commons)
  embeddedKotlin(libs.junit.platform.console)
  embeddedKotlin(libs.junit.platform.engine)
  embeddedKotlin(files(embeddedKotlinRuntime))

  // @TODO(sgammon): needed at build time, but not runtime
  embeddedKotlin(libs.kotlin.serialization.embedded)
  embeddedKotlin(libs.kotlin.powerAssert.embedded)

  embeddedJava(libs.jacoco.agent)
  embeddedJava(libs.junit.jupiter.api)
  embeddedJava(libs.junit.jupiter.params)
  embeddedJava(libs.junit.jupiter.engine)
  embeddedJava(libs.junit.platform.commons)
  embeddedJava(libs.junit.platform.console)
  embeddedJava(libs.slf4j)
  embeddedJava(libs.apiguardian.api)
  embeddedJava(libs.opentest)
  embeddedJava(mn.micronaut.inject.java)
  embeddedJava(libs.jakarta.inject)
  embeddedJava(libs.javax.inject)
  embeddedJava(libs.javax.annotations)
  embeddedJava(libs.asm.core)
  embeddedJava(libs.asm.tree)
  embeddedJava(libs.snakeyaml.core)
  embeddedJava(mn.micronaut.core.processor)
  embeddedJava(mn.reactive.streams)

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
val dependencyRegex = Regex("files-[0-9.]{3}/(.*)/(?:.*)/(.*)")
val kotlinResourceIndex = intermediateResources.map { it.file("kotlin-resources.json") }

@Serializable
sealed interface KotlinResource {
  val artifact: String
  val coordinate: String
  val sha256: String
}

@Serializable
data class KotlinDependencyResource(
  override val coordinate: String,
  override val artifact: String,
  override val sha256: String,
) : KotlinResource

@Serializable
data class KotlinBuiltinResource(
  override val artifact: String,
  override val sha256: String,
) : KotlinResource {
  override val coordinate: String get() = "elide:${artifact.replace(".jar", "")}"
}

@Serializable
data class KotlinResourceIndex(
  val resources: List<KotlinResource>,
) {
  operator fun plus(other: KotlinResourceIndex): KotlinResourceIndex {
    return KotlinResourceIndex(
      resources = (this.resources + other.resources).distinct().sortedBy { it.coordinate },
    )
  }

  fun toPlainObject(): Map<String, List<Map<String, String>>> {
    return resources.map { resource ->
      mapOf(
        "artifact" to resource.artifact,
        "coordinate" to resource.coordinate,
        "sha256" to resource.sha256,
      )
    }.let {
      mapOf(
        "resources" to it,
      )
    }
  }
}

fun indexedDeps(cfg: Configuration): KotlinResourceIndex {
  val seenDeps = TreeSet<String>()
  val seenHashes = TreeSet<String>()
  return cfg.resolve().map { dep ->
    if (dep.absolutePath in seenDeps) {
      error("Path ${dep.absolutePath} already seen")
    } else {
      seenDeps.add(dep.absolutePath)
    }
    val matched = dependencyRegex.find(dep.absolutePath)
    // original:
    // files-2.1/io.micronaut/micronaut-inject-kotlin/4.8.11/.../micronaut-inject-kotlin-4.8.11.jar
    // extracts as:
    // Group 1: io.micronaut/micronaut-inject-kotlin/4.8.11
    // Group 2: micronaut-inject-kotlin-4.8.11.jar
    val coordinatePath = matched?.groups?.get(1)?.value
    val artifact = matched?.groups?.get(2)?.value
    val fingerprint = MessageDigest.getInstance("SHA-256").let {
      val bytes = dep.absoluteFile.readBytes()
      it.update(bytes)
      it.digest()
    }
    val b64 = Base64.getEncoder().encodeToString(fingerprint)
    if (b64 in seenHashes) {
      error("Hash $b64 already seen: ${dep.absolutePath}")
    } else {
      seenHashes.add(b64)
    }

    when {
      coordinatePath == null || artifact == null -> {
        KotlinBuiltinResource(
          artifact = dep.name,
          sha256 = b64,
        )
      }
      else -> {
        val coordinate = coordinatePath.split("/").joinToString(":")
        KotlinDependencyResource(
          coordinate = coordinate,
          artifact = artifact,
          sha256 = b64,
        )
      }
    }
  }.sortedBy { it.coordinate }.let {
    KotlinResourceIndex(
      resources = it,
    )
  }
}

@OptIn(ExperimentalSerializationApi::class) val indexKotlinResources by tasks.registering {
  enabled = false

  indexedDeps(embeddedKotlin).plus(indexedDeps(embeddedJava)).let { index ->
    kotlinResourceIndex.get().asFile.parentFile.mkdirs()
    kotlinResourceIndex.get().asFile.writeText(
      Json {
        prettyPrint = true
        prettyPrintIndent = "  "
      }.encodeToString(
        index.toPlainObject()
      )
    )
  }
  outputs.file(kotlinResourceIndex)
}

val prepKotlinResources by tasks.registering(Copy::class) {
  from(embeddedKotlin + embeddedJava) {
    rename {
      if (it.startsWith("kotlin-stdlib") || it.startsWith("kotlin-reflect") || it.startsWith("kotlin-script-runtime")) {
        // remove version tag from each jar
        val name = it.split(".").first().split("-")
          .dropLast(1).joinToString("-")
        if (name.isEmpty()) {
          it
        } else {
          "$name.jar"
        }
      } else {
        it
      }
    }
  }
  destinationDir = intermediateResources.get().dir("lib").asFile
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  // finalizedBy("indexKotlinResources")
}

val ktRuntimeTarget = "META-INF/elide/embedded/runtime/kt"
val ktRuntimeRoot = layout.projectDirectory.dir("src/main/resources/$ktRuntimeTarget")
val buildKotlinResourcesArchive by tasks.registering(Zip::class) {
  dependsOn(prepKotlinResources)
  // dependsOn(indexKotlinResources)

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
  dependsOn(
    prepKotlinResources,
    buildKotlinResourcesArchive,
    buildResourcesManifest,
    // indexKotlinResources,
  )

  from(kotlinResourceIndex) {
    into(ktRuntimeTarget)
  }
  from(intermediateResources.get().dir("lib")) {
    into(ktRuntimeTarget)
  }
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
  jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.addAll(javacFlags.map { "-Xjavac-arguments=$it" })
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(javacFlags)
}
