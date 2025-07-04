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

package dev.elide.intellij.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.util.io.awaitExit
import dev.elide.intellij.Constants
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Project-level service responsible for interfacing with the Elide CLI binary. Methods in this class expose relevant
 * CLI commands using coroutines and provide a wrapper for the result with [ElideProcess].
 */
@Service(Service.Level.PROJECT)
class ElideCliService(private val project: Project, private val serviceScope: CoroutineScope) {
  /**
   * Encapsulates a call to the Elide CLI running as a subprocess launched by [ProcessBuilder]. Awaiting this value
   * will return the exit code of the process once it finishes.
   *
   * Use [readStdOut] and [readStdErr] to consume the entirety of the process's output and error streams, or manually
   * collect the [stdOut] and [stdErr] flows for better control.
   */
  class ElideProcess private constructor(
    private val process: Process,
    processScope: CoroutineScope,
    exitCode: Deferred<Int>
  ) : Deferred<Int> by exitCode {
    /**
     * Shared flow connected to the process's standard output stream; a buffered reader is used to emit each line from
     * a background thread while subscribers are active.
     */
    val stdOut: Flow<String> = flow {
      process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { emit(it) }
      }
    }.flowOn(Dispatchers.IO).shareIn(processScope, SharingStarted.WhileSubscribed())

    /**
     * Shared flow connected to the process's standard error stream; a buffered reader is used to emit each line from
     * a background thread while subscribers are active.
     */
    val stdErr: Flow<String> = flow {
      process.errorStream.bufferedReader().useLines { lines ->
        lines.forEach { emit(it) }
      }
    }.flowOn(Dispatchers.IO).shareIn(processScope, SharingStarted.WhileSubscribed())

    /** Read the process's standard output stream in its entirety, suspending until it is closed. */
    suspend fun readStdOut() = withContext(Dispatchers.IO) {
      process.inputStream.bufferedReader().readText()
    }

    /** Read the process's standard error stream in its entirety, suspending until it is closed. */
    suspend fun readStdErr() = withContext(Dispatchers.IO) {
      process.errorStream.bufferedReader().readText()
    }

    companion object {
      /**
       * Launch the Elide CLI binary using [ProcessBuilder] and wrap it as an [ElideProcess]. This function is meant
       * to be used by the [ElideCliService], which automatically handles path resolution and provides an execution
       * context.
       */
      fun launch(
        elideBinary: Path,
        projectPath: String,
        scope: CoroutineScope,
        buildCommand: MutableList<String>.() -> Unit
      ): ElideProcess {
        val command = buildList {
          add(elideBinary.toCanonicalPath())
          buildCommand()
        }

        val process = ProcessBuilder(command)
          .directory(File(projectPath))
          .start()

        return ElideProcess(process, scope, scope.async { process.awaitExit() })
      }
    }
  }

  /**
   * Launch the Elide CLI binary as a subprocess and wrap it in [ElideProcess] for easy handling. The settings of the
   * linked project at [projectPath] are used to select an Elide distribution to be invoked.
   */
  private fun launchElide(projectPath: String, buildCommand: MutableList<String>.() -> Unit): ElideProcess {
    val elideHome = ElideDistributionResolver.getElideHome(project, projectPath)
    val elideBin = elideHome.resolve(Constants.ELIDE_BINARY)

    return ElideProcess.launch(elideBin, projectPath, serviceScope, buildCommand)
  }

  /** Invoke the Elide CLI with the `--version` option in a background context and return its output. */
  fun version(projectPath: String): Deferred<String> = serviceScope.async {
    val elide = launchElide(projectPath, buildCommand = { add("--version") })
    elide.readStdOut()
  }
}
