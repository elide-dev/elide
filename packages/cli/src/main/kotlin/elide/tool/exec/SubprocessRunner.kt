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

@file:Suppress("NOTHING_TO_INLINE")

package elide.tool.exec

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.graalvm.nativeimage.ImageInfo
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.io.path.name
import kotlin.sequences.filter
import kotlin.sequences.map
import kotlin.sequences.sortedBy
import elide.tooling.Environment
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tooling.cli.Statics
import elide.tooling.runner.ProcessRunner
import elide.tooling.runner.ProcessRunner.ProcessStatus
import elide.tooling.runner.ProcessRunner.ProcessTaskInfo

// Extra PATH directories where tools may be found.
private val extraProjectPaths = arrayOf(
  "bin",
  ".bin",
  "node_modules/.bin",
  ".venv/bin",
  ".dev/bin",
)

/**
 * # Sub-process Runner
 */
object SubprocessRunner {
  interface CommandLineProcessTask: ProcessRunner.ProcessTask
  interface CommandLineProcessTaskBuilder: ProcessRunner.ProcessTaskBuilder

  @JvmStatic suspend fun CommandContext.stringToTask(
    spec: String,
    shell: ProcessRunner.ProcessShell = ProcessRunner.ProcessShell.Active,
    workingDirectory: Path = Path.of(System.getProperty("user.dir")),
  ): CommandLineProcessTaskBuilder {
    val cwd = Path.of(System.getProperty("user.dir"))
    val toolname = spec.substringBefore(' ')
    val argsStr = spec.substringAfter(' ')
    val argsArr = argsStr.split(' ')
    val argsTrimmed = argsArr.asSequence().map { it.trim() }.filter { it.isNotEmpty() && it.isNotBlank() }
    val toolpath = Path.of(toolname)
    val resolvedToolpath = when {
      // use an identical path to elide so that versions always match.
      // @TODO in jvm mode, this falls through and calls into native elide
      toolname == "elide" && ImageInfo.inImageCode() -> Statics.binPath
      toolname.startsWith(".") -> cwd.resolve(toolpath)
      else -> toolpath
    }
    suspend fun resolvedTool(): Path {
      return if (Files.exists(resolvedToolpath)) {
        resolvedToolpath
      } else {
        which(resolvedToolpath) ?: resolvedToolpath
      }
    }
    return subprocess(resolvedTool(), shell = shell, workingDirectory = workingDirectory) {
      // add all cli args
      args.addAllStrings(argsTrimmed.toList())
    }
  }

  @JvmStatic suspend fun CommandContext.runTask(spec: CommandLineProcessTaskBuilder): CommandLineProcessTask {
    val task = withContext(Dispatchers.IO) {
      ProcessRunner.spawnDefault(spec)
    }
    return object: CommandLineProcessTask, ProcessTaskInfo by spec {
      override fun asDeferred(): Deferred<ProcessStatus> = task.asDeferred()
      override fun status(): ProcessStatus = task.status()
      override fun process(): Process = task.process()
      override fun handle(): ProcessHandle = task.handle()
      override fun pid(): Long = task.pid()
    }
  }

  @JvmStatic suspend fun CommandContext.delegateTask(task: CommandLineProcessTaskBuilder): CommandResult {
    return when (val procStatus = runTask(task).asDeferred().await()) {
      ProcessStatus.Pending, ProcessStatus.Running -> error("Process is still running; should have exited by now")
      ProcessStatus.Success -> success()
      is ProcessStatus.Err -> err(message = procStatus.err.message, exitCode = -1)
      is ProcessStatus.ExitCode -> err(message = "Process exited with non-zero status", exitCode = procStatus.code)
      is ProcessStatus.ExitSignal -> err(message = "Process exited with unexpected signal")
    }
  }

  @JvmStatic suspend fun CommandContext.runTask(task: String): CommandLineProcessTask {
    return runTask(stringToTask(task))
  }

  @JvmStatic fun CommandContext.subprocess(
    exec: Path,
    shell: ProcessRunner.ProcessShell = ProcessRunner.ProcessShell.None,
    workingDirectory: Path = Path.of(System.getProperty("user.dir")),
    block: CommandLineProcessTaskBuilder.() -> Unit,
  ): CommandLineProcessTaskBuilder {
    val out = ProcessRunner.build(exec) {
      // set process options with shell and working directory
      options = ProcessRunner.ProcessOptions(shell = shell, workingDirectory = workingDirectory)

      // default environment overrides PATH
      env = Environment.HostEnv.extend(
        "PATH" to allProjectPaths.joinToString(":") { it.toString() },
      )
      val shim = object: CommandLineProcessTaskBuilder, ProcessRunner.ProcessTaskBuilder by this {
        // nothing at this time
      }
      block.invoke(shim)
    }
    return object: CommandLineProcessTaskBuilder, ProcessRunner.ProcessTaskBuilder by out {
      // nothing at this time
    }
  }
}

private fun Sequence<String>.filterBinDirs(local: Boolean = false): Sequence<Path> {
  val cwd = Path.of(System.getProperty("user.dir"))
  return mapIndexed { idx, it -> idx to it }
    .mapNotNull { (idx, path) ->
      idx to try {
        if (local) {
          cwd.resolve(path)
        } else {
          Path.of(path)
        }
      } catch (_: Exception) {
        return@mapNotNull null
      }
    }
    .filter { (_, path) ->
      Files.exists(path) && Files.isDirectory(path)
    }
    .sortedBy { (idx, _) -> idx }
    .map { (_, path) -> path }
}

@PublishedApi internal val resolvedExecutableCache: Cache<String, Path> by lazy {
  CacheBuilder.newBuilder()
    .maximumSize(100)
    .build()
}

@PublishedApi internal val systemPath: List<Path> by lazy {
  assert(!ImageInfo.inImageBuildtimeCode()) {
    "System PATH is not available at build time"
  }
  System.getenv("PATH")
    .split(':')
    .asSequence()
    .filterBinDirs()
    .toList()
}

@PublishedApi internal val projectBinpaths: List<Path> by lazy {
  extraProjectPaths.asSequence()
    .filterBinDirs(local = true)
    .toList()
}

@PublishedApi internal val allProjectPaths: List<Path> by lazy {
  projectBinpaths.asSequence()
    .plus(systemPath.asSequence())
    .toList()
}

suspend inline fun CommandContext.which(tool: Path, withExtraPaths: Boolean = true): Path? {
  return when (val cached = resolvedExecutableCache.getIfPresent(tool)) {
    null -> whichAll(tool, withExtraPaths).firstOrNull()?.let { found ->
      resolvedExecutableCache.put(tool.name, found)
      return found
    }

    else -> cached
  }
}

suspend inline fun CommandContext.whichAll(tool: Path, withExtraPaths: Boolean = true): Flow<Path> {
  return (if (withExtraPaths) allProjectPaths else systemPath)
    .asFlow()
    .map { path ->
      path.resolve(tool)
    }.filter {
      withContext(Dispatchers.IO) {
        Files.exists(it) && Files.isExecutable(it)
      }
    }
}
