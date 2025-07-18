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
package dev.elide.intellij.cli

import com.intellij.openapi.project.Project
import com.intellij.util.io.awaitExit
import dev.elide.intellij.Constants
import dev.elide.intellij.InvalidElideHomeException
import dev.elide.intellij.service.ElideDistributionResolver
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.io.path.isRegularFile

/**
 * Bridge service used to invoke the Elide CLI on a configured distribution. Use [ElideCommandLine.at] to manually set
 * the path to the distribution or [ElideCommandLine.resolve] to automatically retrieve the path from the project
 * settings.
 */
class ElideCommandLine private constructor(
  private val elideHome: Path,
  private val workDir: Path? = null,
) {
  /**
   * Launch the Elide CLI binary as a subprocess and suspend until it finishes executing. The supplied [block] will
   * be called after the process is launched, and can be used to interact with it in any way; note that it is not
   * necessary to use [awaitExit] in [block], as it is already called before returning.
   *
   * Calling this method directly is discouraged in favor of command-specific extensions like [install], which provide
   * type-safe handling of arguments.
   */
  suspend operator fun <R> invoke(
    buildCommand: MutableList<String>.() -> Unit,
    block: suspend CoroutineScope.(Process) -> R
  ): R {
    val elideBin = elideHome.resolve(Constants.ELIDE_BINARY)
    if (!elideBin.isRegularFile()) throw InvalidElideHomeException(elideHome)

    val command = buildList {
      add(elideBin.toString())
      buildCommand()
    }

    val process = ProcessBuilder(command)
      .also { if (workDir != null) it.directory(workDir.toFile()) }
      .start()

    return coroutineScope {
      val result = async { block(process) }

      suspendCancellableCoroutine { continuation ->
        @Suppress("UsePlatformProcessAwaitExit")
        process.onExit().whenComplete { _, _ -> continuation.resume(Unit) }
        continuation.invokeOnCancellation { process.destroy() }
      }

      result.await()
    }
  }

  companion object {
    /** Returns an [ElideCommandLine] at the given [elideHome], optionally using [workDir] when invoking commands. */
    @JvmStatic fun at(elideHome: Path, workDir: Path? = null): ElideCommandLine {
      return ElideCommandLine(elideHome, workDir)
    }

    /** Returns an [ElideCommandLine] configured according to a linked external project's settings. */
    @JvmStatic fun resolve(project: Project, externalProjectPath: String, workDir: Path? = null): ElideCommandLine {
      return at(ElideDistributionResolver.getElideHome(project, externalProjectPath), workDir)
    }
  }
}


/**
 * Launch the Elide CLI binary as a subprocess and suspend until it finishes executing. The supplied [block] will
 * be called after the process is launched, and can be used to interact with it in any way; note that it is not
 * necessary to use [awaitExit] in [block], as it is already called before returning.
 */
suspend inline operator fun <R> ElideCommandLine.invoke(
  vararg commands: String,
  noinline block: suspend CoroutineScope.(Process) -> R
): R {
  return invoke({ addAll(commands) }, block)
}

/**
 * Calls `elide install`, optionally passing [`--with=sources`][withSources] and [`--with=docs`][withDocs], and returns
 * the exit code. The [useProcess] block can be used to access the output streams for monitoring the operation.
 */
suspend fun ElideCommandLine.install(
  withSources: Boolean = true,
  withDocs: Boolean = true,
  useProcess: suspend CoroutineScope.(Process) -> Unit = {},
): Int {
  return invoke(
    buildCommand = {
      add("install")
      if (withSources) add("--with=sources")
      if (withDocs) add("--with=docs")
    },
  ) {
    useProcess(it)
    it.awaitExit()
  }
}
