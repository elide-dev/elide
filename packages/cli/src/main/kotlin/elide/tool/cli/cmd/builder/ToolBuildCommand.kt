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

import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.animation.progress.addTask
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.text
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.io.path.name
import kotlin.time.Duration.Companion.milliseconds
import elide.exec.Task
import elide.exec.TaskGraph
import elide.exec.TaskGraphEvent.*
import elide.exec.TaskId
import elide.exec.execute
import elide.exec.on
import elide.tool.asArgumentString
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.exec.SubprocessRunner
import elide.tool.exec.SubprocessRunner.delegateTask
import elide.tool.exec.SubprocessRunner.stringToTask
import elide.tool.project.ProjectManager
import elide.tooling.project.ElideProject
import elide.tooling.config.BuildConfiguration
import elide.tooling.config.BuildConfigurators

internal suspend fun CommandContext.emitCommand(task: SubprocessRunner.CommandLineProcessTaskBuilder) {
  output {
    appendLine("$ ${task.executable.name} ${task.args.asArgumentString()}")
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
  @Inject private lateinit var projectManager: ProjectManager

  @CommandLine.Option(
    names = ["-d", "--dry-run"],
    description = ["Don't actually run any tasks"],
  )
  internal var dryRun: Boolean = false

  /** Script file arguments. */
  @CommandLine.Parameters(
    index = "0",
    description = ["Tasks or scripts to build or run"],
    scope = CommandLine.ScopeType.LOCAL,
    arity = "0..*",
    paramLabel = "TASK",
  )
  internal var tasks: List<String>? = null

  // Terminal to use.
  private val terminal by lazy { Terminal() }

  // Top-level "auto-build" entrypoint.
  private suspend fun CommandContext.buildAsProject(name: String, project: ElideProject): CommandResult {
    // configure the build
    val start = System.currentTimeMillis()
    val config = runCatching {
      BuildConfiguration.create(project.root).also {
        BuildConfigurators.contribute(project.load(), it)
      }
    }.onSuccess {
      if (verbose) {
        val done = System.currentTimeMillis()
        val elapsed = done - start
        output { append("Configured build in ${elapsed}ms") }
      }
    }.onFailure {
      if (state()?.output?.verbose == true) {
        output { appendLine(it.stackTraceToString()) }
      }
    }
    return when (config.isFailure) {
      true -> err().also {
        output {
          when (val exc = config.exceptionOrNull()) {
            null -> append("Failed to configure project '$name': Unknown error")
            else -> {
              exc.printStackTrace()
              append("Failed to configure project '$name': ${exc::class.java.simpleName}(${exc.message})")
            }
          }
        }
      }
      false -> config.getOrThrow().let { buildConfig ->
        logging.debug { "Resolved build configuration: $buildConfig" }

        val progress = MultiProgressBarAnimation(
          terminal = terminal,
          clearWhenFinished = true,
        ).animateInCoroutine()

        // prepare layout
        var currentMessage = "Building '$name'..."
        val overallLayout = progressBarLayout(alignColumns = false) {
          text { currentMessage }
          progressBar(width = 20, completeStyle = terminal.theme.success)
        }
        val taskLayout = progressBarContextLayout<String> {
          text(fps = animationFps, align = TextAlign.LEFT) { "〉 $context" }
        }

        val overall = progress.addTask(overallLayout)

        // Top-level build task
        val topTask = progress.addTask(
          taskLayout,
          completed = 0,
          total = 3,
          context = "Preparing project...",
        )

        // begin layout animation
        launch { progress.execute() }

        val graph = async {
          TaskGraph.build(buildConfig.taskGraph)
        }

        val depsTask = progress.addTask(
          taskLayout,
          context = "Resolving dependencies...",
        )

        // finalize configurations
        logging.debug { "Configuring dependency resolvers" }
        val resolvers = async {
          buildConfig.resolvers.all().toList().also {
            it.forEach { it.second.seal() }
          }
        }

        // launch task to resolve deps
        progress.refresh()

        // seal (finally configure) all dependency resolvers
        val allResolvers = resolvers.await()

        var completedResolvers = 0L
        allResolvers.map { resolver ->
          depsTask.update {
            context = "Resolving ${resolver.second.ecosystem.name} dependencies"
          }
          resolver.second.resolve(this).toList().also { suite ->
            suite.forEach {
              depsTask.update {
                completed = ++completedResolvers
              }
            }
          }
        }.flatten().joinAll()

        depsTask.update {
          context = "Dependencies ready"
          completed = ++completedResolvers
        }
        progress.refresh()
        delay(16.milliseconds)

        logging.debug { "Entering graph scope" }
        val execution = coroutineScope {
          launch {

            // we are finished resolving dependencies
            topTask.advance(1L)
            depsTask.also { it.advance() }
          }

          // seal the task graph now, and then execute it, delegating events to output
          topTask.update { context = "Configuring build..." }

          graph.await().execute(buildConfig.actionScope) {
            logging.trace { "Binding graph events" }
            val mappedTasks: MutableMap<TaskId, Task> = mutableMapOf()
            val mappedProgress: MutableMap<TaskId, ProgressTask<String>> = mutableMapOf()
            val taskStarts: MutableMap<TaskId, Long> = mutableMapOf()
            val finishedTasks: MutableList<ProgressTask<*>> = mutableListOf()

            on(Configured) {
              logging.debug { "Task graph configured in ${System.currentTimeMillis() - start}ms" }
              topTask.update { context = "Building project..." }
              progress.refresh(true)
            }
            on(TaskReady) {
              val task = context as Task
              logging.trace { "[build] Task ready for execution: $task" }
              mappedTasks[task.id] = task
            }
            on(TaskExecute) {
              val task = context as Task
              logging.debug { "[build] Task executing: $task" }
              progress.addTask(
                taskLayout,
                context = task.describe(),
              ).also {
                mappedProgress[task.id] = it
                taskStarts[task.id] = System.currentTimeMillis()
              }
            }
            on(TaskFinished) {
              val task = context as Task
              logging.debug { "[build] Task finished: $task" }
              val progressTask = mappedProgress.remove(task.id)
              val startedAt = taskStarts.remove(task.id)

              progressTask?.let {
                if (startedAt != null) {
                  it.update { context = "Completed in ${System.currentTimeMillis() - startedAt}ms" }
                }
                it.advance(1L)
              }
              mappedTasks.remove(task.id)
              finishedTasks.add(progressTask ?: return@on)
            }
            on(ExecutionFinished) {
              // remove all pending tasks
              logging.debug { "[build] Execution finished for graph" }
              finishedTasks.forEach {
                progress.removeTask(it.id)
              }
            }
          }
        }

        // wait for graph to fully complete
        logging.debug { "[build] Awaiting graph completion" }
        execution.await()
        logging.debug { "[build] Graph completed; drawing finishing UI" }
        topTask.advance(topTask.total ?: 1L)
        overall.advance()

        val totalMs = System.currentTimeMillis() - start
        currentMessage = "✅ Build succeeded in ${totalMs}ms"
        progress.removeTask(depsTask.id)
        progress.removeTask(topTask.id)
        progress.refresh(true)
        logging.debug { "[build] Finished with all build tasks; clearing and succeeding" }

        progress.clear()
        output { appendLine(currentMessage) }
        success()
      }
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val project = projectManager.resolveProject(projectOptions().projectPath) ?: return CommandResult.err(
      message = "No valid Elide project found, nothing to build"
    )

    // resolve project name and version; use folder name if no project name is specified.
    val projectName = project.manifest.name ?: project.root.name
    if (verbose) {
      when (val version = project.manifest.version) {
        null -> output { append("Building '$projectName'...") }
        else -> output { append("Building '$projectName' (v$version)...") }
      }
    }

    // if the dev has specified a `build` script, prefer that. otherwise, try to run the automatic builder, which infers
    // build steps based on project configuration.
    return when (val buildScript = project.manifest.scripts["build"]) {
      // with no build step, we pass the configuration to the automatic builder.
      null -> buildAsProject(projectName, project)

      // with a build step, we consume the specified script string and delegate to it in full.
      else -> stringToTask(buildScript).let { task ->
        // emit to stdout
        emitCommand(task)

        // in this case, we fully delegate this command's progress to the spawned task.
        return delegateTask(task)
      }
    }
  }
}
