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

package elide.tool.cli.cmd.deps

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.micronaut.context.BeanContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.nio.file.Path
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.Elide
import elide.tool.cli.ProjectAwareSubcommand
import elide.tooling.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.builder.BuildOutput
import elide.tool.cli.output.redirectLoggingToMordant
import elide.tooling.project.ProjectManager
import elide.tooling.builder.BuildDriver.dependencies
import elide.tooling.builder.BuildDriver.resolve
import elide.tooling.config.BuildConfiguration
import elide.tooling.config.BuildConfigurator.*
import elide.tooling.config.BuildConfigurators
import elide.tooling.config.on
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.MavenClassifier
import elide.tooling.lockfile.ElideLockfile
import elide.tooling.lockfile.InterpretedLockfile
import elide.tooling.lockfile.LockfileDefinition
import elide.tooling.lockfile.Lockfiles
import elide.tooling.project.ElideProject
import elide.tooling.project.load

@CommandLine.Command(
  name = "install",
  aliases = ["i"],
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, resolve and install all dependencies. " +
    "Dependencies are installed for all declared ecosystems, modulo provided flags.",
    "",
    "Running @|bold elide install|@ without arguments installs all dependencies visible to the project." +
    "Running @|bold elide install <ecosystem...>|@ installs the transitive closure for each specified ecosystem.",
    "",
    "Project structure and dependencies are managed via @|bold elide.pkl|@, or via foreign manifests such as " +
      "@|bold package.json|@.",
    "",
    "Supported ecosystems include:",
    "  - @|bold,fg(cyan) npm|@ (Node.js)",
    "  - @|bold,fg(cyan) pip|@ (Python)",
    "  - @|bold,fg(cyan) maven|@ (Maven)",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) install|@",
    "   or: elide @|bold,fg(cyan) install|@ [OPTIONS] [ECOSYSTEM...] [--] [ARGS]",
    "   or: elide @|bold,fg(cyan) install|@ [@|bold,fg(cyan) -p|@/@|bold,fg(cyan) --project|@=<path>] [OPTIONS] " +
            "[ECOSYSTEM...]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class InstallCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  private companion object {
    private val DEFAULT_LOCKFILE_FORMAT = ElideLockfile.Format.BINARY
  }

  @Inject private lateinit var beanContext: BeanContext
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>

  @CommandLine.Option(
    names = ["--progress"],
    negatable = true,
    defaultValue = "true",
    description = ["Show progress indicators and animations; activated by default where supported"],
  )
  internal var showProgress: Boolean = true

  @CommandLine.Option(
    names = ["--with"],
    description = ["Extras to install, like 'sources' and/or 'docs'"],
  )
  internal var installExtras: List<MavenClassifier> = listOf()

  @CommandLine.Option(
    names = ["--lockfile-format"],
    defaultValue = "BINARY",
    description = ["Force a different lockfile format to be written"],
  )
  internal var lockfileFormat: ElideLockfile.Format = DEFAULT_LOCKFILE_FORMAT

  // Progress controller to use.
  private lateinit var buildOutput: BuildOutput

  private fun prepareBuilderOutput(scope: CommandContext) = Statics.terminal.let { terminal ->
    if (showProgress && terminal.terminalInfo.interactive) {
      terminal.redirectLoggingToMordant()
      buildOutput = BuildOutput.animated(scope, terminal, verbose)
    } else {
      buildOutput = BuildOutput.serial(scope, terminal, pretty, verbose)
    }
  }

  // Install dependencies for an Elide project.
  @Suppress("TooGenericExceptionCaught")
  private suspend fun CommandContext.installDepsForProject(project: ElideProject): CommandResult = coroutineScope {
    prepareBuilderOutput(this@installDepsForProject)
    val start = TimeSource.Monotonic.markNow()
    buildOutput.status { "Configuring project" }

    BuildConfiguration.create(project.root).let { config ->
      val opts = projectOptions()
      val loadedProject = project.load()
      config.settings.sources = MavenClassifier.Sources in installExtras
      config.settings.docs = MavenClassifier.Docs in installExtras
      if (installExtras.isNotEmpty()) {
        buildOutput.status { "Installing with extras: ${installExtras.joinToString(", ")}" }
      }
      BuildConfigurators.contribute(beanContext, loadedProject, config, binder = {
        on<TaskState, MetadataDownloaded>(MetadataDownloaded) { ctx ->
          // on-metadata-downloaded
          buildOutput.debug { "Metadata downloaded '${ctx.label}'" }
        }
        on<TaskState, ArtifactDownloaded>(ArtifactDownloaded) { ctx ->
          // on-artifact-downloaded
          buildOutput.debug { "Artifact downloaded '${ctx.label}'" }
        }
        on<TaskState, ResolutionStart>(ResolutionStart) {
          buildOutput.debug { requireNotNull(it.label) }
        }
        on<TaskState, ResolutionFinished>(ResolutionFinished) {
          buildOutput.status { requireNotNull(it.label) }
        }
      })

      try {
        buildOutput.status { "Resolving dependencies" }
        val deps = dependencies(config)
        val (resolvers, jobs) = resolve(config, deps.await())
        jobs.joinAll()
        buildOutput.status { "Dependencies ready" }

        if (opts.useLockfile) {
          val anticipatedOp = measureTimedValue {
            buildAnticipatedLockfile(project, resolvers)
          }
          val anticipated = anticipatedOp.value
          val duration = anticipatedOp.duration
          val existing = loadedProject.activeLockfile
          if (
            existing != null &&
            anticipated.fingerprint.asBytes().compareTo(existing.lockfile.fingerprint.asBytes()) != 0
          ) {
            buildOutput.status { "Rebuilding lockfile" }

            // if the lockfile does not match what's already on disk, we typically need to update it; the exception is
            // frozen mode. if active, we should error instead.
            if (opts.frozenLockfile) {
              return@coroutineScope err("Lockfile is frozen, but needs update")
            }
            writeLockfile(existing.updateTo(duration = duration) { anticipated })
          } else if (existing == null) {
            // we have no existing lockfile, so just write the one we have build.
            buildOutput.status { "Writing fresh lockfile" }
            val outFmt = lockfileFormat
            writeLockfile(
              path = project.root.resolve(".dev").resolve(outFmt.filename).also {
                it.parent?.createDirectories()
              },
              fmt = outFmt,
              lockfile = anticipated,
              version = ElideLockfile.latest(),
            )
          } else {
            buildOutput.status { "Lockfile is up-to-date" }
          }
        }
        if (!quiet) output {
          val now = start.elapsedNow()
          val successMsg = (TextStyles.bold + TextColors.green)("Dependencies ready in $now")
          append("✅ $successMsg")
        }
        success()
      } catch (e: Exception) {
        err("Failed to install dependencies: ${e::class.java.simpleName} ${e.message ?: "(No message)"}", exc = e)
      }
    }
  }

  private suspend fun CommandContext.buildAnticipatedLockfile(
    project: ElideProject,
    resolvers: List<DependencyResolver>,
  ): ElideLockfile = Lockfiles.create(
    project.root,
    project = project,
    resolvers = resolvers,
  )

  private suspend fun CommandContext.writeLockfile(lockfile: InterpretedLockfile) {
    writeLockfile(
      lockfile.root.resolve(".dev").resolve(lockfile.format.filename).also {
        it.parent?.createDirectories()
      },
      lockfile.format,
      lockfile.lockfile,
      lockfile.definition,
    )
  }

  private suspend fun CommandContext.writeLockfile(
    path: Path,
    fmt: ElideLockfile.Format,
    lockfile: ElideLockfile,
    version: LockfileDefinition<*>,
  ) {
    kotlinx.coroutines.withContext(IO) {
      path.outputStream().buffered().use { stream ->
        version.writeTo(
          fmt,
          lockfile,
          stream,
        )
      }
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // tools typically require native access; force early init
    Elide.requestNatives(server = false, tooling = true)

    return when (val project = projectManagerProvider.get().resolveProject(projectOptions().projectPath())) {
      null -> success().also {
        output { append("No resolvable project.") }
      }
      else -> installDepsForProject(project)
    }
  }
}
