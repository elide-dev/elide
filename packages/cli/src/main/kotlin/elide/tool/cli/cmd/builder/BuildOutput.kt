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
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.io.path.name
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource
import elide.exec.Task
import elide.exec.TaskId
import elide.runtime.Logger
import elide.tool.asArgumentString
import elide.tool.cli.CommandContext
import elide.tool.exec.SubprocessRunner

internal interface BuildOutputApi: Logger {
  val isPretty: Boolean
  val isAnimated: Boolean
  suspend fun verbose(message: String)
  suspend fun verbose(messageProducer: BuildOutputApi.() -> String)
  suspend fun status(message: String, pretty: String? = null)
  suspend fun status(messageProducer: BuildOutputApi.() -> String)
  suspend fun failure()
  suspend fun success()
  suspend fun emitCommand(task: SubprocessRunner.CommandLineProcessTaskBuilder)
}

internal interface BuildOutput : BuildOutputApi {
  companion object {
    @JvmStatic fun serial(
      ctx: CommandContext,
      terminal: Terminal,
      pretty: Boolean = true,
      verbose: Boolean = false,
    ): BuildOutput {
      return Serial(pretty, verbose, ctx, terminal)
    }

    @JvmStatic fun animated(ctx: CommandContext, terminal: Terminal, verbose: Boolean = false): BuildOutput {
      return Animated(ctx, terminal, verbose)
    }
  }

  interface TaskEphemera : BuildOutputApi {
    val id: TaskId
  }

  interface TaskOutput : TaskEphemera
  interface TransferOutput : TaskEphemera

  suspend fun taskScope(task: Task): TaskOutput
  suspend fun transferScope(task: Task): TransferOutput
}

suspend fun CommandContext.emitCommand(task: SubprocessRunner.CommandLineProcessTaskBuilder) {
  output {
    append("$ ${task.executable.name} ${task.args.asArgumentString()}")
  }
}

private abstract class BaseOutput (
  override val isPretty: Boolean,
  val ctx: CommandContext,
  val terminal: Terminal,
  val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : BuildOutput, Logger by ctx.logging {
  // Initial build start.
  private val outStart = timeSource.markNow()

  fun markNow(): ComparableTimeMark = timeSource.markNow()
  fun timeSince(): Duration = markNow() - outStart

  private fun renderMessage(message: String): String = timeSince().let { elapsed ->
    buildString {
      if (isPretty) {
        append(TextStyles.dim("[${elapsed.relative()}]"))
      } else {
        append("[${elapsed.relative()}]")
      }
      append(" ")
      append(message)
    }
  }

  abstract suspend fun emitCommand(rendered: String)
  abstract suspend fun emitVerbose(rendered: String)
  abstract suspend fun emitStatus(rendered: String)

  override suspend fun emitCommand(task: SubprocessRunner.CommandLineProcessTaskBuilder) {
    emitCommand("$ ${task.executable.name} ${task.args.asArgumentString()}")
  }

  override suspend fun verbose(message: String) = ctx.output {
    ctx.logging.info(renderMessage(message))
  }

  override suspend fun verbose(messageProducer: BuildOutputApi.() -> String) = ctx.output {
    ctx.logging.info {
      renderMessage(messageProducer.invoke(this@BaseOutput))
    }
  }

  override suspend fun status(message: String, pretty: String?) {
    emitStatus(renderMessage(if (isPretty && pretty != null) pretty else message))
  }

  override suspend fun status(messageProducer: BuildOutputApi.() -> String) = ctx.output {
    append(renderMessage(messageProducer.invoke(this@BaseOutput)))
  }

  override suspend fun failure() {

  }

  override suspend fun success() {

  }

  override suspend fun taskScope(task: Task): BuildOutput.TaskOutput  {
    return object: BuildOutput.TaskOutput, BuildOutputApi by this {
      override val id: TaskId get() = task.id

      override suspend fun failure() {

      }

      override suspend fun success() {

      }
    }
  }

  override suspend fun transferScope(task: Task): BuildOutput.TransferOutput {
    return object: BuildOutput.TransferOutput, BuildOutputApi by this {
      override val id: TaskId get() = task.id

      override suspend fun failure() {

      }

      override suspend fun success() {

      }
    }
  }
}

private class Animated (
  ctx: CommandContext,
  terminal: Terminal,
  private val verbose: Boolean,
) : BaseOutput(true, ctx, terminal) {
  private inner class ProgressContext {}
  private inner class TransferContext {}

  override val isAnimated: Boolean get() = true
  private val activeContext = atomic(ProgressContext())

  // Overall layout.
  private val layout = progressBarLayout {
    // render progress bar layout
  }

  // Top-level build task.
  private val topTask = taskLayout()

  // Main progress renderer.
  private val progress = layout.animateInCoroutine(
    terminal = terminal,
  )

  // Create a new progress bar context for the given transfer.
  private fun transferLayout() = progressBarContextLayout<TransferContext> {
    // render progress bar layout for transfer
  }

  // Create a new progress bar context for the given task.
  private fun taskLayout() = progressBarContextLayout<ProgressContext> {
    // render progress bar layout for task
  }

  override suspend fun emitCommand(rendered: String) = ctx.output { terminal.println(rendered) }
  override suspend fun emitStatus(rendered: String) = ctx.output { terminal.println(rendered) }
  override suspend fun emitVerbose(rendered: String) = if (verbose) {
    terminal.println(rendered)
  } else {
    ctx.logging.info(rendered)
  }

  override suspend fun taskScope(task: Task): BuildOutput.TaskOutput {
    // context = activeContext.value
    return super.taskScope(task)
  }
}

private class Serial (
  pretty: Boolean,
  private val verbose: Boolean,
  ctx: CommandContext,
  terminal: Terminal,
) : BaseOutput(pretty, ctx, terminal) {
  override val isAnimated: Boolean get() = false

  override suspend fun emitCommand(rendered: String) = ctx.output { append(rendered) }
  override suspend fun emitStatus(rendered: String) = ctx.output { append(rendered) }
  override suspend fun emitVerbose(rendered: String) {
    if (verbose) ctx.output { append(rendered) } else ctx.logging.info(rendered)
  }
}
