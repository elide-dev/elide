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

package elide.tooling.containers

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.JibContainer
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.cli.CommonCliOptions
import com.google.cloud.tools.jib.cli.Credentials
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.plugins.common.DefaultCredentialRetrievers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers.IO
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue
import elide.exec.ActionScope
import elide.exec.Task
import elide.exec.Task.Companion.fn
import elide.exec.taskDependencies
import elide.tooling.Arguments
import elide.tooling.Classpath
import elide.tooling.ClasspathSpec
import elide.tooling.MultiPathUsage
import elide.tooling.cli.Statics
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurator.BuildConfiguration
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.gvm.nativeImage.NativeImageBuildConfigurator
import elide.tooling.jvm.JarBuildConfigurator
import elide.tooling.jvm.JvmBuildConfigurator
import elide.tooling.jvm.JvmLibraries
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.project.manifest.ElidePackageManifest.*

// Target container image and coordinate.
private typealias TargetImage = Pair<ContainerImage, ContainerCoordinate>

// Where native app binaries are placed in container images.
private const val NATIVE_CONTAINER_APP_PATH = "/app/"

/**
 * ## Default Base Images
 *
 * Base container images to use in various conditions, as defaults; if the user specifies a base image, these are never
 * used or considered.
 */
public data object DefaultBaseImages {
  /**
   * ### JVM Base Image (Oracle)
   *
   * Specifies a base multi-platform image that is capable of running JVM applications.
   */
  public const val ORACLE_JVM: BaseImage = "container-registry.oracle.com/graalvm/jdk:24"

  /**
   * ### JVM Base Image (Distroless)
   *
   * Specifies a Distroless Java base image from Google, at JVM 21.
   */
  public const val DISTROLESS_JVM_21: BaseImage = "gcr.io/distroless/java21-debian12"

  /**
   * ### JVM Base Image (Default)
   *
   * Specifies the default JVM base image.
   */
  public const val JVM: BaseImage = DISTROLESS_JVM_21

  /**
   * ### Native Base Image
   *
   * Specifies a base multi-platform image that is capable of running native applications.
   */
  public const val NATIVE: BaseImage = "ghcr.io/elide-dev/runtime/native:latest"
}

/**
 * ## Container Build Configurator
 */
internal class ContainerBuildConfigurator : BuildConfigurator {
  override fun dependsOn(): List<KClass<out BuildConfigurator>> = listOf(
    JvmBuildConfigurator::class,
    JarBuildConfigurator::class,
    NativeImageBuildConfigurator::class,
  )

  // Resolve the coordinate for the target container.
  private fun resolveCoordinate(state: ElideBuildState, root: Path, img: ContainerImage): ContainerCoordinate {
    return (img.image ?: state.manifest.name ?: root.name).let {
      ContainerCoordinate.parse(it)
    }
  }

  // Resolve task dependencies to satisfy before the container is built.
  private fun resolveTaskDeps(state: ElideBuildState): List<Task> {
    val built = state.config.taskGraph.build()
    return built.nodes().filter {
      // @TODO better than strings?
      it.toString().lowercase().let {
        // all jvm build tasks, all jars, and all native image tasks
        it.contains("kotlin") || it.contains("java") || it.contains("jar") || it.contains("native")
      }
    }
  }

  // Resolve the artifact configuration to wrap in the container image.
  private fun resolveFromArtifact(name: String, state: ElideBuildState, container: ContainerImage): Artifact {
    val fromArtifacts = container.from.map { fromSpec ->
      state.manifest.artifacts[fromSpec] ?: error(
        "No such artifact '$fromSpec' to build container '$name' from"
      )
    }
    return when {
      fromArtifacts.isEmpty() -> error(
        "Building a container currently requires at least one artifact. Declare this via a `from { ... }` block " +
        "in your container configuration."
      )
      fromArtifacts.size > 1 -> error(
        "Building a container currently does not support multiple artifacts; please only declare one in " +
        "your `from { ... }` block. Found: ${fromArtifacts.joinToString(", ")}"
      )
      else -> fromArtifacts.first()
    }
  }

  // Resolve the default base container image to use for a project, if any.
  private fun defaultBaseContainerIfAny(from: Artifact): String? = when (from) {
    is Jar -> DefaultBaseImages.JVM
    is NativeImage -> DefaultBaseImages.NATIVE
    else -> null // no default base image for other artifact types
  }

  // Resolve the base coordinate for the container image, if specified.
  private fun resolveBase(from: Artifact, container: ContainerImage): ContainerCoordinate? {
    return (container.base ?: defaultBaseContainerIfAny(from))?.let {
      ContainerCoordinate.parse(it)
    }
  }

  // Calculate default/injected JVM flags.
  private fun defaultJvmFlags(state: ElideBuildState): List<String> {
    return listOf(
      "-Delide.project.name=${state.project.manifest.name ?: "(unset)"}",
      "-Delide.project.version=${state.project.manifest.version ?: "0.0.0"}",
    )
  }

  // Configure a JVM container build according to project state.
  private suspend fun ElideBuildState.jvmImage(build: JavaContainerBuilder, jar: Jar) = build.apply {
    // prepare project outputs dependencies if present
    val resolver = config.resolvers[DependencyResolver.MavenResolver::class] as? MavenAetherResolver
    val sourceOutPaths = jar.sources.map {
      layout
        .artifacts
        .resolve("jvm")
        .resolve("classes")
        .resolve(it)  // source set name
    }
    val entrypoint = (jar.options.entrypoint ?: project.manifest.jvm?.main)?.ifBlank { null } ?: error(
      "Failed to resolve main class; this is required to build a JVM container image. " +
      "Please specify a main class via `jvm { main = ... }`, or within the `jar` artifact."
    )

    // build classpath
    val classpath = Classpath.empty().toMutable().apply {
      // add classpath from project dependency resolver
      resolver?.classpathProvider(object : ClasspathSpec {
        override val usage: MultiPathUsage = MultiPathUsage.Runtime
      })?.classpath()?.let {
        add(it)
      }

      // add classpath from builtins
      add(JvmLibraries.builtinClasspath(resourcesPath))
    }

    val effectiveJvmFlags: Arguments = Arguments.empty().toMutable().apply {
      addAllStrings(defaultJvmFlags(this@jvmImage))

      project.manifest.jvm?.flags?.let { flags ->
        addAllStrings(flags)
      }
    }.build()

    build.apply {
      // add primary class out paths
      sourceOutPaths.forEach { addClasses(it) }

      // add classpath dependencies
      addDependencies(classpath.map { it.path.absolute() }.toList())

      // entrypoint class
      setMainClass(entrypoint)

      // add jvm flags
      effectiveJvmFlags.asArgumentStrings().forEach { addJvmFlag(it) }
    }
  }

  // Configure target container information.
  private fun buildTargetContainerizer(
    state: ElideBuildState,
    ref: ImageReference,
    target: TargetImage?,
    reg: RegistryImage?,
  ): Containerizer {
    return if (target == null) {
      // build to a codebase-local tarball
      Containerizer.to(TarImage.at(
        state.layout
          .artifacts
          .resolve("containers")
          .resolve("container.tar")
      ).named(ref))
    } else {
      val (_, coordinate) = target
      when (coordinate.registry) {
        null -> Containerizer.to(DockerDaemonImage.named(ref))
        else -> Containerizer.to(requireNotNull(reg) { "Expected a `RegistryImage`" })
      }
    }
  }

  // Drive a configured container build to a terminal state.
  private suspend fun ElideBuildState.build(target: TargetImage, jib: JibContainerBuilder) = withContext(IO) {
    // configures auth
    val ref = ImageReference.parse(target.second.asString())
    val reg = RegistryImage.named(ref)
    val defaultCredRetrievers = DefaultCredentialRetrievers.init(
      CredentialRetrieverFactory.forImage(ref) { logEvent ->
        val logging = Statics.logging
        if (logEvent.message?.startsWith("Using credentials from") == true) {
          // skip this message, as it is implied here
          return@forImage
        }
        when (logEvent.level) {
          LogEvent.Level.LIFECYCLE -> logging.info { logEvent.message }
          LogEvent.Level.WARN -> logging.warn { logEvent.message }
          LogEvent.Level.ERROR -> logging.error { logEvent.message }
          LogEvent.Level.INFO, LogEvent.Level.DEBUG -> logging.debug { logEvent.message }
          null, LogEvent.Level.PROGRESS -> {}  // @TODO eventing?
        }
      }
    )
    val commonCliOptions = CommonCliOptions()
    Credentials.getFromCredentialRetrievers(commonCliOptions, defaultCredRetrievers).forEach { retriever ->
      reg.addCredentialRetriever(retriever)
    }

    runCatching {
      // if we are running in dry or non-deploy mode, build to a tarball
      val dontPush = !config.settings.deploy || config.settings.dry
      val effectiveTarget = when (dontPush) {
        false -> target
        true -> null
      }

      @Suppress("UsePropertyAccessSyntax")
      jib.containerize(buildTargetContainerizer(this@build, ref, effectiveTarget, reg).apply {
        setToolName("elide")
        setToolVersion("beta")  // @TODO actual version
      })
    }
  }

  // Build a container which wraps a built JVM artifact.
  private suspend fun ElideBuildState.jvmContainer(image: TargetImage, jib: JibContainerBuilder): JibContainer {
    // TODO("jar-based container images")
    return build(image, jib).getOrThrow()
  }

  // Build a container which wraps a built native image artifact.
  private suspend fun ElideBuildState.nativeContainer(
    artifact: NativeImage,
    image: TargetImage,
    jib: JibContainerBuilder,
  ): JibContainer {
    // locate the artifact binary
    val imageName = artifact.name?.ifBlank { null } ?: project.manifest.name ?: config.projectRoot.name
    val (doWalk, imageBinaryLocation) = layout
      .artifacts
      .resolve("native-image")
      .resolve(imageName).let { top ->
        top.isDirectory() to top
      }

    val executableFiles = HashMap<Path, Boolean>()
    val paths = sequence {
      if (!doWalk) {
        yield(imageBinaryLocation to "/app/${imageBinaryLocation.name}")
      } else {
        Files.walk(imageBinaryLocation).toList().forEach { path ->
          if (Files.isRegularFile(path)) {
            if (Files.isExecutable(path)) {
              executableFiles[path] = true
            }
            yield(path to imageBinaryLocation.relativize(path))
          }
        }
      }
    }.toList()

    return build(image, jib.apply {
      addFileEntriesLayer(FileEntriesLayer.builder().apply {
        setName("app")
        paths.forEach { (path, at) ->
          val prefixed = "/$NATIVE_CONTAINER_APP_PATH/$at"
          addEntry(path, AbsoluteUnixPath.get(prefixed), if (executableFiles.containsKey(path)) {
            FilePermissions.fromPosixFilePermissions(setOf(
              PosixFilePermission.OWNER_READ,
              PosixFilePermission.OWNER_EXECUTE,
              PosixFilePermission.GROUP_READ,
              PosixFilePermission.GROUP_EXECUTE,
              PosixFilePermission.OTHERS_EXECUTE,
            ))
          } else {
            FilePermissions.DEFAULT_FILE_PERMISSIONS
          })
        }
      }.build())

      setEntrypoint(
        Paths.get(NATIVE_CONTAINER_APP_PATH, imageName).absolutePathString()
      )
    }).getOrThrow()
  }

  /**
   * ### Container Image Task
   *
   * Create a task within the current [ActionScope] which builds a container image.
   *
   * @param name Name of the task to create.
   * @param state Current build state.
   * @param config Build configuration.
   * @param image Container image to build.
   * @param from Artifact to wrap in the container image.
   * @param coordinate Target container image coordinate.
   * @param dependencies Optional list of tasks that this task depends on.
   * @return A new task that builds a container image.
   */
  private fun ActionScope.containerImage(
    name: String,
    state: ElideBuildState,
    config: BuildConfiguration,
    image: ContainerImage,
    from: Artifact,
    coordinate: ContainerCoordinate,
    dependencies: List<Task> = emptyList(),
    base: ContainerCoordinate? = null,
  ) = fn(name, taskDependencies(dependencies)) {
    // resolve base image to builder
    measureTimedValue {
      runCatching {
        when (from) {
          // jar-based container images are based on JVM and built with classpath awareness for layering
          is Jar -> state.jvmContainer(
            image to coordinate,
            when (base) {
              null -> JavaContainerBuilder.from(DefaultBaseImages.JVM)
              else -> JavaContainerBuilder.from(base.asString())
            }.let { state.jvmImage(it, from) }.toContainerBuilder()
          )

          // native image containers use a thin base image and have no classpath by definition
          is NativeImage -> state.nativeContainer(
            from,
            image to coordinate,
            when (base) {
              null -> Jib.fromScratch()
              else -> Jib.from(base.asString())
            },
          )

          else -> error("Don't yet know how to build from artifact of type ${from::class.simpleName}")
        }
      }
    }.let { timedResult ->
      val result = timedResult.value
      val elapsed = timedResult.duration

      if (result.isSuccess) {
        val container = result.getOrThrow()
        val didPush = container.isImagePushed
        val label = TextColors.green(if (didPush) "Pushed" else "Built")
        val postfix = TextStyles.dim("in ${elapsed.inWholeSeconds}s →")
        Statics.terminal.println(
          buildString {
            append("✅ $label $postfix")
            append(" ")
            if (didPush) {
              append(TextStyles.bold(container.targetImage.toString()))
              append('@')
              append(container.digest)
            } else {
              append("container ")
              append(TextStyles.bold(container.targetImage.toString()))
            }
          }
        )
        elide.exec.Result.Nothing
      } else {
        val exc = result.exceptionOrNull()
        logging.error("Failed to build container image", exc)
        if (exc != null) {
          elide.exec.Result.ThrowableFailure(exc)
        } else {
          elide.exec.Result.UnspecifiedFailure
        }
      }
    }
  }.describedBy {
    "Building container ${TextStyles.bold(coordinate.asString())}"
  }.also {
    config.taskGraph.addNode(it)
    dependencies.forEach { dep ->
      config.taskGraph.putEdge(it, dep)
    }
  }

  override suspend fun contribute(state: ElideBuildState, config: BuildConfiguration) {
    state.manifest.artifacts.entries.filter { it.value is ContainerImage }.forEach { (name, container) ->
      config.actionScope.apply {
        config.taskGraph.apply {
          resolveFromArtifact(name, state, container as ContainerImage).let { artifact ->
            containerImage(
              name,
              state,
              config,
              container,
              artifact,
              resolveCoordinate(state, config.projectRoot, container),
              resolveTaskDeps(state),
              resolveBase(artifact, container),
            ).also {
              logging.debug { "Configured container build for name '$name'" }
            }
          }
        }
      }
    }
  }
}
