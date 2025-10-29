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

@file:Suppress("LongMethod")
@file:OptIn(ExperimentalCoroutinesApi::class)

package elide.tool.cli.cmd.builder

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.micronaut.context.BeanContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.util.concurrent.ConcurrentSkipListMap
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue
import elide.exec.Task
import elide.exec.TaskGraph
import elide.exec.TaskGraphEvent.*
import elide.exec.TaskId
import elide.exec.execute
import elide.exec.on
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.Elide
import elide.tool.cli.ProjectAwareSubcommand
import elide.tooling.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.cli.output.redirectLoggingToMordant
import elide.tool.exec.SubprocessRunner.delegateTask
import elide.tool.exec.SubprocessRunner.stringToTask
import elide.tooling.project.ProjectManager
import elide.tooling.BuildMode
import elide.tooling.builder.BuildDriver
import elide.tooling.builder.BuildDriver.dependencies
import elide.tooling.builder.BuildDriver.resolve
import elide.tooling.project.ElideProject

internal fun Duration.relative(): String {
  toComponents { h, m, s, _ ->
    return when {
      s == 0 -> "${inWholeMilliseconds}ms"
      h > 0 -> "${h}h${m}m${s}s"
      s > 0 -> "${s}s${inWholeMilliseconds.minus(s.seconds.inWholeMilliseconds)}ms"
      m > 0 -> "${m}m${s}s"
      else -> toString()
    }
  }
}

@CommandLine.Command(
  name = "build",
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, run the build, or a script mapped at the name " +
      "'build' within @|bold elide.pkl|@, or project manifests like @|bold package.json|@.",
    "",
    "Running @|bold elide build|@ without arguments builds all tasks in the project's graph." +
      "Running @|bold elide build <task...>|@ runs the specified task(s) and their dependencies.",
    "",
    "After the `--` token, any arguments passed via the command-line are considered arguments" +
      " to the build. Such arguments are made available to executing tasks. Argument files are" +
      " supported and may be passed as @|bold @<file>|@.",
    "",
    "Project structure and dependencies are managed via @|bold elide.pkl|@.",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) build|@",
    "   or: elide @|bold,fg(cyan) build|@ [OPTIONS] [TASKS] [--] [ARGS]",
    "   or: elide @|bold,fg(cyan) build|@ [@|bold,fg(cyan) -p|@/@|bold,fg(cyan) --project|@=<path>] [OPTIONS] " +
      "[TASKS] [--] [ARGS]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class ToolBuildCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var beanContext: BeanContext
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  private val projectManager: ProjectManager get() = projectManagerProvider.get()

  @CommandLine.Option(
    names = ["-d", "--dry"],
    description = ["Don't actually run any tasks"],
  )
  internal var dryRun: Boolean = false

  @CommandLine.Option(
    names = ["--release"],
    description = ["Enable release mode (shorthand for `--build-mode=release`)"],
    defaultValue = "false",
  )
  internal var release: Boolean = false

  @CommandLine.Option(
    names = ["--deploy"],
    negatable = true,
    description = ["Whether to deploy artifacts, as applicable"],
    defaultValue = "false",
  )
  internal var deploy: Boolean = release

  @CommandLine.Option(
    names = ["--mode"],
    description = ["Exact build mode to use for this build."],
  )
  internal var buildModeParam: BuildMode? = null

  @CommandLine.Option(
    names = ["--cache"],
    negatable = true,
    defaultValue = "true",
    description = ["Enable or disable build + dependency caching."],
  )
  internal var enableCaching: Boolean = true

  @CommandLine.Option(
    names = ["--dependencies"],
    negatable = true,
    defaultValue = "true",
    description = ["Enable or disable dependency management."],
  )
  internal var enableDeps: Boolean = true

  @CommandLine.Option(
    names = ["--check"],
    negatable = true,
    defaultValue = "true",
    description = ["Enable or disable checks during build."],
  )
  internal var enableChecks: Boolean = true

  @CommandLine.Option(
    names = ["--progress"],
    negatable = true,
    defaultValue = "true",
    description = ["Show progress indicators and animations; activated by default where supported"],
  )
  internal var showProgress: Boolean = true

  /** Names of specific build tasks to run. */
  @CommandLine.Parameters(
    index = "0",
    description = ["Tasks/targets or flags"],
    scope = CommandLine.ScopeType.LOCAL,
    arity = "0..*",
    paramLabel = "TASK|FLAG",
  )
  internal var params: List<String>? = null

  /** Command-line usage, as parsed, with no processing. */
  @CommandLine.Spec internal lateinit var spec: CommandLine.Model.CommandSpec

  // Terminal build error, if any.
  private val buildErr = atomic<CommandResult?>(null)

  // Progress controller to use.
  private lateinit var buildOutput: BuildOutput

  // Effective build mode, resolved from all parameters and other state.
  private val effectiveBuildMode: BuildMode by lazy {
    // if the user has specified a build mode, use that.
    buildModeParam ?: when {
      release -> BuildMode.Release
      System.getenv("NODE_ENV") == "production" -> BuildMode.Release
      else -> BuildMode.Development
    }
  }

  private fun prepareBuilderOutput(scope: CommandContext) = Statics.terminal.let { terminal ->
    if (showProgress && terminal.terminalInfo.interactive) {
      terminal.redirectLoggingToMordant()
      buildOutput = BuildOutput.animated(scope, terminal, verbose)
    } else {
      buildOutput = BuildOutput.serial(scope, terminal, pretty, verbose)
    }
  }

  private suspend inline fun <T> timedStep(message: String, block: () -> T): T {
    return TimeSource.Monotonic.measureTimedValue { block() }.also {
      buildOutput.status { message }
    }.value
  }

  // Top-level "auto-build" entrypoint.
  private suspend fun CommandContext.buildProject(project: ElideProject): CommandResult = coroutineScope {
    // configure the build
    prepareBuilderOutput(this@buildProject)
    var sawFailures = false
    buildOutput.status { "Configuring project" }
    val config = BuildDriver.configure(beanContext, project) { _, config ->
      config.settings.caching = enableCaching
      config.settings.dependencies = enableDeps
      config.settings.checks = enableChecks
      config.settings.buildMode = effectiveBuildMode
      config.settings.release = effectiveBuildMode == BuildMode.Release
      config.settings.debug = effectiveBuildMode == BuildMode.Debug
      config.settings.deploy = deploy
    }
    val taskMap = ConcurrentSkipListMap<TaskId, BuildOutput.TaskOutput>()
    timedStep("Dependencies ready") {
      val deps = dependencies(config).await()
      val (_, jobs) = resolve(config, deps)
      jobs.joinAll()
    }

    if (dryRun) {
      buildOutput.status { "Skipping build (dry-run mode active)" }
      success()
    } else TaskGraph.build(config.taskGraph).execute(config.actionScope) {
      on(Configured) {
        buildOutput.debug { "Graph configured: $context" }
      }
      on(TaskReady) {
        val task = context as Task
        taskMap[task.id] = buildOutput.taskScope(task)
        buildOutput.debug { "Task ready for execution: $context" }
      }
      on(TaskExecute) {
        val task = context as Task
        val scope = requireNotNull(taskMap[task.id])
        scope.status { task.describe() }
        scope.success()
      }
      on(TaskCompleted) {
        val task = context as Task
        val scope = requireNotNull(taskMap[task.id])
        scope.success()
      }
      on(TaskFailed) {
        val task = context as Task
        val scope = requireNotNull(taskMap[task.id])
        scope.verbose { "Failed '${task.id}'" }
        scope.failure()
        sawFailures = true
      }
      on(ExecutionFailed) {
        sawFailures = true
        buildOutput.debug { "Graph failed: $context" }
        buildOutput.status {
          when (isPretty) {
            true -> TextStyles.bold(TextColors.red("✗ Build failed"))
            false -> "Build failed"
          }
        }
        buildOutput.failure()
      }
      on(ExecutionCompleted) {
        buildOutput.debug { "Graph completed: $context" }
        buildOutput.status {
          when (isPretty) {
            true -> TextStyles.bold(TextColors.green("✓ Build successful"))
            false -> "Build successful"
          }
        }
        buildOutput.success()
      }
    }.await().let {
      buildErr.value ?: when (sawFailures) {
        true -> err("Build failed", silent = true)
        false -> success()
      }
    }
  }

  @Suppress("ReturnCount")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    Elide.requestNatives(server = false, tooling = true)

    val isDebug = effectiveBuildMode == BuildMode.Debug
    val isRelease = effectiveBuildMode == BuildMode.Release
    val inputParams = params

    val project = projectManager.resolveProject(
      projectOptions().projectPath(),
      object: ProjectManager.ProjectEvaluatorInputs {
        override val debug: Boolean get() = isDebug
        override val release: Boolean get() = isRelease
        override val params: List<String>? get() = inputParams
      }
    ) ?: return CommandResult.err(
      message = "No valid Elide project found, nothing to build"
    )

    // resolve project name and version; use folder name if no project name is specified.
    val projectName = project.manifest.name ?: project.root.name
    when (val version = project.manifest.version) {
      null -> output { append("Building $projectName") }
      else -> output { append("Building $projectName (v$version)...") }
    }

    // if the dev has specified a `build` script, prefer that. otherwise, try to run the automatic builder, which infers
    // build steps based on project configuration.
    return when (val buildScript = project.manifest.scripts["build"]) {
      // with no build step, we pass the configuration to the automatic builder.
      null -> buildProject(project)

      // with a build step, we consume the specified script string and delegate to it in full.
      else -> stringToTask(buildScript).let { task ->
        // emit to stdout
        buildOutput.emitCommand(task)

        // in this case, we fully delegate this command's progress to the spawned task.
        if (dryRun) {
          if (verbose) {
            output {
              append("Skipping command (dry-run mode is active).")
            }
          }
          return success()
        }
        return delegateTask(task)
      }
    }
  }
}
