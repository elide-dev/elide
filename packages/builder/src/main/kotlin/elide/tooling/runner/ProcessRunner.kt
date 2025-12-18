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
package elide.tooling.runner

import java.nio.file.Path
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import elide.tooling.Arguments
import elide.tooling.Environment
import elide.tooling.MutableArguments

/**
 * # Process Runner
 */
public object ProcessRunner {
  /**
   * ## Process Shell
   */
  public sealed interface ProcessShell {
    public data object Active : ProcessShell
    public data object None : ProcessShell

    @JvmInline public value class ByPath(public val path: Path) : ProcessShell
  }

  /**
   * ## Process Options
   */
  @JvmRecord public data class ProcessOptions(
    public val shell: ProcessShell,
    public val workingDirectory: Path,
  )

  /**
   * ## Standard Streams
   */
  public sealed interface StdStreams {
    public data object Defaults : StdStreams
  }

  /**
   * ## Process Status
   */
  public sealed interface ProcessStatus {
    public data object Pending : ProcessStatus
    public data object Running : ProcessStatus

    @JvmInline public value class Err(public val err: Throwable) : ProcessStatus

    public sealed interface Exit : ProcessStatus
    public data object Success : Exit

    @JvmInline public value class ExitCode(public val code: Int) : Exit

    @JvmInline public value class ExitSignal(public val signal: String) : Exit
  }

  /**
   * ## Process Task Info
   */
  public interface ProcessTaskInfo {
    public val executable: Path
    public val options: ProcessOptions
    public val args: Arguments
    public val env: Environment
    public val streams: StdStreams
  }

  /**
   * ## Process Task
   */
  public interface ProcessTask : ProcessTaskInfo {
    public fun status(): ProcessStatus
    public fun asDeferred(): Deferred<ProcessStatus>
    public fun pid(): Long
    public fun handle(): ProcessHandle
    public fun process(): Process
  }

  /**
   * ## Process Task Builder
   */
  public interface ProcessTaskBuilder : ProcessTaskInfo {
    override var executable: Path
    override var options: ProcessOptions
    override var args: MutableArguments
    override var env: Environment
    override var streams: StdStreams
    public suspend fun spawn(): ProcessTask
  }

  @JvmStatic public fun build(exec: Path, block: ProcessTaskBuilder.() -> Unit): ProcessTaskBuilder {
    val mutArgs = Arguments.empty().toMutable()
    val mutEnv = Environment.empty().toMutable()
    val builder = buildFrom(exec, mutArgs, mutEnv)
    block.invoke(builder)
    return builder
  }

  @Suppress("TooGenericExceptionCaught")
  @JvmStatic public suspend fun spawnDefault(task: ProcessTaskBuilder): ProcessTask {
    var currentStatus: ProcessStatus = ProcessStatus.Pending
    val toolpath = task.executable.absolutePathString()
    val useShell = when (val shell = task.options.shell) {
      is ProcessShell.None -> null
      is ProcessShell.ByPath -> shell.path
      is ProcessShell.Active -> System.getenv("SHELL")?.ifBlank { null }?.let {
        Path.of(it).absolute()
      }
    }
    val resolvedArgs = (listOf(toolpath) + task.args.asArgumentList()).let { baseArgs ->
      when (useShell) {
        null -> baseArgs
        else -> listOf(useShell.absolutePathString(), "-c", baseArgs.joinToString(" "))
      }
    }
    val procBuilder = ProcessBuilder(resolvedArgs).apply {
      // handle working directory
      directory(task.options.workingDirectory.toFile())

      // handle task environment
      when (task.env) {
        // inject host environment
        is Environment.HostEnv -> environment().putAll(System.getenv())

        else -> environment().let { targetEnv ->
          task.env.entries.forEach { (k, v) ->
            targetEnv[k] = v
          }
        }
      }

      // handle task streams
      when (task.streams) {
        is StdStreams.Defaults -> inheritIO()
      }
    }

    val started = procBuilder.start()
    val job: Deferred<ProcessStatus> = coroutineScope {
      async {
        try {
          started.waitFor()
        } catch (txe: TimeoutException) {
          currentStatus = ProcessStatus.Err(txe)
          return@async currentStatus
        } catch (throwable: Throwable) {
          currentStatus = ProcessStatus.Err(throwable)
          return@async currentStatus
        }
        currentStatus = when (val exitCode = started.exitValue()) {
          0 -> ProcessStatus.Success
          else -> ProcessStatus.ExitCode(exitCode)
        }
        currentStatus
      }
    }
    return object : ProcessTask, ProcessTaskInfo by task {
      override fun status(): ProcessStatus = currentStatus
      override fun asDeferred(): Deferred<ProcessStatus> = job
      override fun pid(): Long = started.pid()
      override fun handle(): ProcessHandle = started.toHandle()
      override fun process(): Process = started
    }
  }

  @JvmStatic public fun buildFrom(
    exec: Path,
    args: Arguments.Suite,
    env: Environment,
    streams: StdStreams? = null,
    options: ProcessOptions? = null,
  ): ProcessTaskBuilder {
    val mutArgs = args.toMutable()
    val mutEnv = env.toMutable()
    var executablePath: Path = exec
    var effectiveStreams: StdStreams = streams ?: StdStreams.Defaults
    var effectiveOptions: ProcessOptions = options ?: ProcessOptions(
      shell = ProcessShell.None,
      workingDirectory = Path.of(System.getProperty("user.dir")),
    )

    return object : ProcessTaskBuilder {
      override var executable: Path = executablePath
      override var options: ProcessOptions = effectiveOptions
      override var args: MutableArguments = mutArgs
      override var env: Environment = mutEnv
      override var streams: StdStreams = effectiveStreams
      override suspend fun spawn(): ProcessTask = spawnDefault(this)
    }
  }
}
