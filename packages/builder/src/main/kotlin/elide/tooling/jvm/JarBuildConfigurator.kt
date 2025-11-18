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
@file:Suppress("UnstableApiUsage")

package elide.tooling.jvm

import com.github.ajalt.mordant.rendering.TextStyles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.Manifest
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.reflect.KClass
import kotlin.streams.asSequence
import elide.exec.ActionScope
import elide.exec.Task
import elide.exec.Task.Companion.fn
import elide.exec.taskDependencies
import elide.runtime.Logging
import elide.tooling.Arguments
import elide.tooling.Classpath
import elide.tooling.Environment
import elide.tooling.AbstractTool
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.*
import elide.tooling.project.ElideProject
import elide.tooling.project.SourceSetLanguage
import elide.tooling.project.SourceSetType
import elide.tooling.project.manifest.ElidePackageManifest.*

/**
 * ## Jar Configurator
 *
 * Describes the context of a configurator function.
 */
public interface JarConfigurator {}

/**
 * ## Jar Build Configurator
 */
internal class JarBuildConfigurator : BuildConfigurator {
  private val logging by lazy { Logging.of(JarBuildConfigurator::class) }

  override fun dependsOn(): List<KClass<out BuildConfigurator>> = listOf(
    JvmBuildConfigurator::class,
  )

  // Resolve named source sets for the JAR artifact to paths.
  private fun resolveSourceSetsForJar(state: ElideBuildState, jar: Jar): List<Pair<String, Path>> {
    val specifiedSourceSets = jar.sources.ifEmpty { null } ?: listOf("main")
    val sourceSets = specifiedSourceSets.map {
      state.project.sourceSets[it] ?: error(
        "Source set '$it' (specified for JAR '${jar.name}') does not exist in project source sets",
      )
    }
    return sourceSets.map { sourceSet ->
      // @TODO non-jvm source sets?
      sourceSet.name to state.layout.artifacts
        .resolve("jvm")
        .resolve("classes")
        .resolve(sourceSet.name)
    }
  }

  // Resolve extra paths for resources which should be placed in the JAR; the string is the path within the JAR.
  private fun resolveExtraPathsForJar(state: ElideBuildState, jar: Jar): List<Pair<Path, String>> {
    val root = state.config.projectRoot
    val resources = jar.resources
    return resources.map { resource ->
      val pathKey = if (resource.key.startsWith("/")) resource.key else {
        logging.debug { "Resource key for JAR '${resource.key}' is not absolute; implying '/'" }
        "/${resource.key}"
      }
      val path = root.resolve(resource.value.path)
      if (!Files.exists(path)) {
        error(buildString {
          append("Resource '${resource.value.path}'")
          append("(specified for JAR '")
          append(jar.name ?: "default")
          append("' does not exist in project root")
        })
      }
      path to pathKey
    }
  }

  // Resolve the suite of tasks that should precede the JAR task. Typically class and resource prep tasks.
  private fun taskDepsForJar(state: ElideBuildState): List<Task> {
    val built = state.config.taskGraph.build()
    return built.nodes().filter {
      // @TODO better than strings?
      it.toString().lowercase().let { token ->
        token.contains("kotlin") || token.contains("java")
      }
    }
  }

  // Build the final JAR file name which should be written.
  private fun buildJarFileName(name: String, state: ElideBuildState, artifact: Jar): String = buildString {
    val jarName = (
      artifact.name ?: name.takeIf {
        it != "jar" && it != "main" && it.isNotEmpty() && it.isNotBlank()
      } ?: state.project.manifest.name ?: state.config.projectRoot.name
    ).removeSuffix(".jar")

    // @TODO qualifiers, etc
    append(jarName)
    append(".jar")
  }

  /**
   * ### Jar Task
   *
   * Create a task within the current [ActionScope] which builds a JAR (Java Archive) file as an output; JAR information
   * is passed either via parameters to this function, or amended via [jarConfigurator], which may be provided by the
   * caller.
   *
   * @param name Name of the task to create.
   * @param state Current build state.
   * @param config Build configuration.
   * @param artifact The [NativeImage] artifact to build; this should be an entry in the current build state manifest.
   * @param srcSets Source sets to include in the JAR (built classes).
   * @param classpath Classpath to declare within the JAR.
   * @param extraPaths Extra resources to include within the JAR.
   * @param dependencies Tasks which this task depends on.
   * @param manifest Entries to include in the JAR's manifest.
   * @param jarConfigurator Optional configurator to amend the JAR task.
   * @return A [Task] which builds a JAR file according to the provided parameters.
   */
  @Suppress("TooGenericExceptionCaught", "LongParameterList", "unused")
  private fun ActionScope.jar(
    name: String,
    state: ElideBuildState,
    config: BuildConfiguration,
    artifact: Jar,
    srcSets: List<Pair<String, Path>> = emptyList(),
    extraPaths: List<Pair<Path, String>> = emptyList(),
    dependencies: List<Task> = emptyList(),
    manifest: Map<String, String> = emptyMap(),
    classpath: Classpath? = null,
    stampManifest: Boolean = true,
    injectManifest: Boolean = true,
    stampClasspath: Boolean = false,
    entrypoint: String? = null,
    jarConfigurator: JarConfigurator.() -> Unit = {},
  ) = buildJarFileName(name, state, artifact).let { jarName ->
    fn(name, taskDependencies(dependencies)) {
      val projectName = state.project.manifest.name?.ifBlank { null } ?: state.config.projectRoot.name
      val projectVersion = state.project.manifest.version
      val jarOut = state.layout
        .artifacts
        .resolve("jvm")
        .resolve("jars")
        .resolve(jarName)

      // main class entry (optional)
      val isMainJar = srcSets.any { (name, _) -> name == "main" }
      val resolvedEntry = entrypoint ?: artifact.options.entrypoint ?: if (!isMainJar) null else {
        state.project.manifest.jvm?.main  // use project-level main class for main jar, if available
      }

      // build finalized JAR manifest
      val finalizedManifest = buildMap<Attributes.Name, String> {
        if (injectManifest) {
          put(Attributes.Name.MANIFEST_VERSION, "1.0")
          put(Attributes.Name("Created-By"), "Elide")
          put(Attributes.Name.IMPLEMENTATION_TITLE, projectName)
          projectVersion?.let { put(Attributes.Name.IMPLEMENTATION_VERSION, it) }

          if (stampManifest) {
            put(Attributes.Name("Build-Timestamp"), System.currentTimeMillis().toString())
          }
          if (resolvedEntry != null) {
            put(Attributes.Name.MAIN_CLASS, resolvedEntry)
          }
          if (stampClasspath && classpath != null) {
            val rendered = classpath.joinToString(":")
            put(Attributes.Name.CLASS_PATH, rendered)
          }
        }

        manifest.forEach { (key, value) ->
          put(Attributes.Name(key), value)
        }
      }
      val jarBuildRoot = Files.createTempDirectory(
        "elide-build-jar-"
      )
      val buildroot = jarBuildRoot.resolve("jar")

      val allPathsToCopy = sequence {
        srcSets.map { (_, path) ->
          yield(path.absolute() to "/")
        }
        extraPaths.map { (path, position) ->
          yield(path to position)
        }
      }.flatMap { (path, position) ->
        when {
          path.isDirectory() -> position.let { Paths.get(it) }.let { base ->
            Files.walk(path).map { sub ->
              val rel = sub.relativeTo(path)
              sub.absolute() to base.resolve(rel).toString()
            }.asSequence()
          }

          else -> sequenceOf(
            path.absolute() to position,
          )
        }
      }.map { (path, position) ->
        Triple(path, buildroot.resolve(position), position)
      }

      val buildrootFile = buildroot.toFile()
      if (!config.settings.preserve) {
        buildrootFile.deleteOnExit()
      }
      val jarEntries = HashMap<String, Path>()

      allPathsToCopy.toList().mapNotNull { (from, relativeTo, position) ->
        // create parent dir for target
        val to = if (relativeTo.toString() == "/")
          return@mapNotNull null else buildroot.resolve(relativeTo.toString().removePrefix("/"))
        val parent = to.parent
        if (parent != null && !parent.exists()) {
          Files.createDirectories(parent)
        }

        // is it a file? if so, copy it. if not, and it's a directory, create it.
        when {
          from.isDirectory() -> Files.createDirectory(to)
          from.isRegularFile() -> Files.copy(from, to)
          else -> error("Cannot copy non-file and non-directory to JAR: $from")
        }
        to to position
      }.map { (from, position) ->
        jarEntries[position] = from
      }

      // write the jar manifest
      val targetManifestPath = buildroot.resolve("META-INF/MANIFEST.MF")
      if (!targetManifestPath.exists()) {
        Files.createDirectories(targetManifestPath.parent)
        Manifest().apply {
          finalizedManifest.forEach { entry ->
            mainAttributes[entry.key] = entry.value
          }
        }.let { manifest ->
          targetManifestPath.outputStream().buffered().use { output ->
            manifest.write(output)
          }
        }
      }

      val jarArgs = Arguments.empty().toMutable().apply {
        add("--create")
        add("--file")
        add(jarOut.absolutePathString())
        if (targetManifestPath.exists()) {
          add("--manifest")
          add(targetManifestPath.absolutePathString())
        }
        add("-C")
        add(buildroot.absolutePathString())
        add(".")
      }

      val jarOuts = JarTool.outputJar(jarOut)
      val jarIns = JarTool.jarFiles(jarEntries.values.asSequence())
      val tool = JarTool(jarArgs, Environment.host(), jarIns, jarOuts)

      try {
        // build the jar
        tool.invoke(
          object : AbstractTool.EmbeddedToolState {
            override val resourcesPath: Path get() = state.resourcesPath
            override val project: ElideProject? get() = state.project
          }
        )
        elide.exec.Result.Nothing
      } catch (err: Throwable) {
        logging.error("Failed to invoke JAR tool", err)
        elide.exec.Result.UnspecifiedFailure
      }
    }.describedBy {
      val bolded = TextStyles.bold(jarName)
      "Packing $bolded"
    }.also { jar ->
      config.taskGraph.apply {
        addNode(jar)
        if (dependencies.isNotEmpty()) {
          dependencies.forEach { dependency ->
            putEdge(jar, dependency)
          }
        }
      }
    }
  }

  override suspend fun contribute(state: ElideBuildState, config: BuildConfiguration) {
    var hasMainJar = false
    state.manifest.artifacts.entries.filter { it.value is Jar }.forEach { (name, jartifact) ->
      config.actionScope.apply {
        config.taskGraph.apply {
          if (name == "main" || name == "jar") hasMainJar = true
          jar(
            name,
            state,
            config,
            jartifact as Jar,
            resolveSourceSetsForJar(state, jartifact),
            resolveExtraPathsForJar(state, jartifact),
            taskDepsForJar(state),
          ).also {
            logging.debug { "Configured JAR build for name '$name'" }
          }
        }
      }
    }
    if (!hasMainJar) {
      val mainSources = state.project.sourceSets.find(SourceSetType.Sources).firstOrNull()
      val langs = mainSources?.languages ?: emptySet()
      if (langs.isNotEmpty()) {
        if (SourceSetLanguage.Kotlin in langs || SourceSetLanguage.Java in langs) {
          // we are configuring a build with the following conditions:
          // 1) there is a main source set which uses one of (Kotlin or Java), but
          // 2) there is no explicitly configured main jar (a jar task called 'main' or 'jar'),
          // so we can create one on behalf of the user.
          config.actionScope.apply {
            val jartifact = Jar(
              name = "main",
              sources = listOf("main"),
              options = JarOptions(
                entrypoint = state.manifest.jvm?.main?.ifEmpty { null }?.ifBlank { null },
              ),
            )
            config.taskGraph.apply {
              jar(
                name = "jar",
                state = state,
                config = config,
                dependencies = taskDepsForJar(state),
                artifact = jartifact,
                srcSets = resolveSourceSetsForJar(state, jartifact),
              )
            }
          }
        }
      }
    }
  }
}
