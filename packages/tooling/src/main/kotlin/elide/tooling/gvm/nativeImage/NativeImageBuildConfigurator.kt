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

package elide.tooling.gvm.nativeImage

import com.github.ajalt.mordant.rendering.TextStyles
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.reflect.KClass
import elide.exec.ActionScope
import elide.exec.Task
import elide.exec.Task.Companion.fn
import elide.exec.taskDependencies
import elide.tool.Argument
import elide.tool.ArgumentContext
import elide.tool.Arguments
import elide.tool.Classpath
import elide.tool.ClasspathSpec
import elide.tool.Environment
import elide.tool.MultiPathUsage
import elide.tooling.AbstractTool
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.BuildConfiguration
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.JarBuildConfigurator
import elide.tooling.jvm.JvmBuildConfigurator
import elide.tooling.jvm.JvmLibraries
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.project.ElideProject
import elide.tooling.project.manifest.ElidePackageManifest.*

/**
 * ## Native Image Build Configurator
 */
internal class NativeImageBuildConfigurator : BuildConfigurator {
  override fun dependsOn(): List<KClass<out BuildConfigurator>> = listOf(
    JvmBuildConfigurator::class,
    JarBuildConfigurator::class,
  )

  // Build a native image output name.
  private fun buildNativeImageName(state: ElideBuildState, root: Path, artifact: NativeImage): String = when {
    artifact.name?.ifEmpty { null }?.ifBlank { null } != null -> artifact.name
    else -> state.manifest.name ?: root.name
  }

  // Resolve the suite of tasks that should precede the Native Image task. All compile and JAR tasks.
  private fun taskDepsForImage(config: BuildConfiguration): List<Task> {
    val built = config.taskGraph.build()
    return built.nodes().filter {
      // @TODO better than strings?
      it.toString().lowercase().let {
        it.contains("kotlin") || it.contains("java") || it.contains("jar")
      }
    }
  }

  // Resolve the classpath to use for the Native Image build task; this should include user dependencies and code.
  private fun classpathForImage(state: ElideBuildState, img: NativeImage): suspend () -> Classpath? = {
    (state.config.resolvers[DependencyResolver.MavenResolver::class] as? MavenAetherResolver)?.let { resolver ->
      Classpath.empty().toMutable().apply {
        // if there are jars defined by the user, we should add them
        val froms = img.from.map {
          // pull from artifacts
          state.manifest.artifacts[it] ?: error(
            "No artifact named '$it' to build a native image from"
          )
        }

        // any from-artifacts which are jars should be added to the classpath
        froms.filterIsInstance<Jar>().map { jar ->
          val jarname = (jar.name ?: state.manifest.name ?: state.config.projectRoot.name).removeSuffix(".jar")
          val jarNameQualified = "$jarname.jar"
          add(state.layout
            .artifacts
            .resolve("jvm")
            .resolve("jars")
            .resolve(jarNameQualified))
        }

        // add classpath from project dependency resolver
        resolver.classpathProvider(object : ClasspathSpec {
          override val usage: MultiPathUsage = MultiPathUsage.Compile
        })?.classpath()?.let {
          add(it)
        }

        // add classpath from builtins
        add(JvmLibraries.builtinClasspath(state.resourcesPath))
      }.let {
        Classpath.from(it.map { it.path })
      }
    }
  }

  // Resolve the class entrypoint for a native image build, unless it's for a shared library.
  private fun resolveEntrypointForNativeImage(state: ElideBuildState, artifact: NativeImage): String? {
    return when (artifact.type) {
      NativeImageType.LIBRARY -> null  // no entrypoint for libraries
      else -> (artifact.entrypoint ?: state.manifest.jvm?.main).also { effective ->
        require(effective != null && effective.isNotEmpty() && effective.isNotBlank()) {
          "No entrypoint specified for native image artifact '${artifact.name}'"
        }
      }
    }
  }

  /**
   * ### Native Image Task
   *
   * Create a task within the current [ActionScope] which builds some output via the Native Image compiler, typically
   * provided by GraalVM.
   *
   * @param name Name of the task to create.
   * @param state Current build state.
   * @param config Build configuration.
   * @param artifact The [NativeImage] artifact to build; this should be an entry in the current build state manifest.
   * @param entrypoint Entrypoint for the native image; this is typically the main class to run; `null` for libraries.
   * @param classpath Optional classpath to use for this task; if not provided, the default classpath will be used.
   * @param dependencies Optional list of tasks that this task depends on.
   * @return A new task that builds a native image.
   */
  private fun ActionScope.nativeImage(
    name: String,
    state: ElideBuildState,
    config: BuildConfiguration,
    artifact: NativeImage,
    entrypoint: String? = null,
    classpath: (suspend () -> Classpath?)? = null,
    dependencies: List<Task> = emptyList(),
    injectArgs: Boolean = true,
  ) = buildNativeImageName(state, config.projectRoot, artifact).let { nativeImageName ->
    fn(name, taskDependencies(dependencies)) {
      val effectiveClasspath = classpath?.invoke() ?: Classpath.empty()
      val imageOutPath = state.layout
        .artifacts
        .resolve("svm")
        .resolve("images")
        .resolve(nativeImageName)

      val argRenderer = ArgumentContext.of(
        argSeparator = ' ',
        kvToken = '=',
      )
      val nativeImageArgs = Arguments.empty().toMutable().apply {
        // add user's extra compile flags
        addAllStrings(artifact.options.flags)

        if (injectArgs) {
          if (artifact.options.verbose) {
            add("--verbose")
          }

          // only supported on linux: debug mode builds
          val isDebugMode = if (System.getProperty("os.name")?.lowercase() != "linux") false else {
            if (!config.settings.debug) false else {
              add("-g")
              true
            }
          }

          // add pgo information if enabled
          val pgoEnabled = if (!artifact.options.pgo.enabled || isDebugMode) false else when {
            // instrumentation comes first, but only in non-release mode
            artifact.options.pgo.instrument && !config.settings.release -> false.also {
              add("--pgo-instrument")
              if (artifact.options.pgo.sampling) {
                add("--pgo-sampling")
              }
            }

            // we are enabled and we have profiles = activate it
            artifact.options.pgo.profiles.isNotEmpty() -> true.also {
              artifact.options.pgo.profiles.joinToString(",") {
                if (File.pathSeparatorChar in it) {
                  // resolve from project root
                  state.config.projectRoot
                    .resolve(it)
                    .absolutePathString()
                } else {
                  // otherwise, pull it from `.dev/profiles`
                  state.layout
                    .devRoot
                    .resolve("profiles")
                    .resolve(it)
                    .absolutePathString()
                }
              }.let {
                add("--pgo=$it")
              }
            }

            // otherwise, pgo is not enabled
            else -> false
          }

          // add optimization level
          add(when (val level = artifact.options.optimization) {
            OptimizationLevel.AUTO -> when {
              config.settings.release -> if (pgoEnabled) OptimizationLevel.THREE else OptimizationLevel.FOUR
              config.settings.debug -> OptimizationLevel.ZERO
              else -> OptimizationLevel.BUILD
            }.let {
              "-O${it.symbol}"
            }

            else -> "-O${level.symbol}"
          })

          // disable fallback mode
          add("--no-fallback")

          // set image name
          add("-o")
          add(nativeImageName)

          // apply link-at-build-time
          if (artifact.options.linkAtBuildTime.packages.isNotEmpty()) {
            add(Argument.of("--link-at-build-time" to artifact.options.linkAtBuildTime.packages.joinToString(",")))
          } else if (artifact.options.linkAtBuildTime.enabled) {
            add("--link-at-build-time")
          }

          // apply class init settings
          if (artifact.options.classInit.enabled) {
            add("--initialize-at-build-time")
          }
          if (artifact.options.classInit.buildtime.isNotEmpty()) {
            add(Argument.of("--initialize-at-build-time" to artifact.options.classInit.buildtime.joinToString(",")))
          }
          if (artifact.options.classInit.runtime.isNotEmpty()) {
            add(Argument.of("--initialize-at-run-time" to artifact.options.classInit.runtime.joinToString(",")))
          }

          effectiveClasspath.takeIf { it.isNotEmpty() }?.let { cp ->
            add(Argument.of("--class-path" to cp.joinToString(":") { it.path.absolutePathString() }))
          }

          // handle libraries or main entrypoints
          if (artifact.type == NativeImageType.LIBRARY) {
            add("--shared")
          } else when (entrypoint?.ifBlank { null }?.ifEmpty { null }) {
            null -> logging.warn { "No entrypoint provided in configuration for Native Image; build may break." }
            else -> add(entrypoint)
          }
        }
      }.build()

      val finalizedNativeImageArgs = nativeImageArgs.asArgumentList(argRenderer)
      logging.debug { "Final native image args: '$finalizedNativeImageArgs'" }

      val inputs = NativeImageDriver.nativeImageInputs(
        effectiveClasspath.asSequence().map { it.path }
      )
      val outputs = NativeImageDriver.outputBinary(
        imageOutPath
      )
      val driver = NativeImageDriver(
        nativeImageArgs,
        Environment.host(),
        inputs,
        outputs,
        projectRoot = config.projectRoot,
      )
      driver.invoke(object: AbstractTool.EmbeddedToolState {
        override val resourcesPath: Path get() = state.resourcesPath
        override val project: ElideProject? get() = state.project
      }).let {
        when (it.success) {
          false -> elide.exec.Result.UnspecifiedFailure
          true -> elide.exec.Result.Nothing
        }
      }
    }.describedBy {
      "Compiling native image ${TextStyles.bold(nativeImageName)}"
    }.also {
      config.taskGraph.addNode(it)
      dependencies.forEach { dep ->
        config.taskGraph.putEdge(it, dep)
      }
    }
  }

  override suspend fun contribute(state: ElideBuildState, config: BuildConfiguration) {
    state.manifest.artifacts.entries.filter { it.value is NativeImage }.forEach { (name, artifact) ->
      config.actionScope.apply {
        config.taskGraph.apply {
          nativeImage(
            name,
            state,
            config,
            artifact as NativeImage,
            resolveEntrypointForNativeImage(state, artifact),
            classpathForImage(state, artifact),
            taskDepsForImage(config),
          ).also {
            logging.debug { "Configured Native Image build for name '$name'" }
          }
        }
      }
    }
  }
}
