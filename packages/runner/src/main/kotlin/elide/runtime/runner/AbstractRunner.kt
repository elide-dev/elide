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
package elide.runtime.runner

import org.graalvm.polyglot.Context
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import elide.runtime.runner.base.RunnerInfoImpl

/**
 * ## Abstract Runner
 *
 * An abstract base class for implementing a [Runner] that can execute jobs of type [T]. This class provides basic
 * functionality for managing the runner's lifecycle, including initialization, job acceptance, and execution.
 */
public abstract class AbstractRunner<T: RunnerJob>(private val name: String) : Runner<T> {
  private val initialized = atomic(false)
  private val closed = atomic(false)
  private val activeContext = atomic<Context?>(null)
  private val coroutines = atomic<CoroutineContext?>(null)
  private val activeJob = atomic<Deferred<RunnerOutcome>?>(null)

  // Use the runner's internals, but only in an active and initialized state.
  private inline fun <R> withActive(block: () -> R): R {
    require(!closed.value) { "Runner is closed" }
    require(initialized.value) { "Runner is not initialized" }
    return block()
  }

  override val info: RunnerInfo get() = RunnerInfoImpl(
    name = name,
  )

  override fun close() {
    closed.value = true
  }

  override fun accept(job: T): Unit = withActive {
    runBlocking {
      activeJob.value = async(requireNotNull(coroutines.value), start = kotlinx.coroutines.CoroutineStart.DEFAULT) {
        invoke(object: RunnerExecution<T> {
          override val job: T = job
          override val context: Context = requireNotNull(activeContext.value) { "Context not ready for execution" }
        })
      }
    }
  }

  override fun configure(context: Context, coroutineContext: CoroutineContext) {
    activeContext.value = context
    coroutines.value = coroutineContext
    initialized.value = true
  }

  override fun poll(): Deferred<RunnerOutcome>? = withActive {
    activeJob.value
  }

  override suspend fun await(): RunnerOutcome = withActive {
    requireNotNull(activeJob.value).await()
  }

  override suspend fun invoke(job: T): RunnerOutcome {
    accept(job)
    return await()
  }

  /**
   * Emit an error as the outcome of the current job.
   *
   * @param message The error message to emit.
   * @param exitCode The exit code to use for the error outcome; if none is specified, this defaults to `1`.
   * @param cause An optional [Throwable] cause for the error.
   * @return A [RunnerOutcome] representing the error.
   */
  protected fun err(message: String, exitCode: Int = 1, cause: Throwable? = null): RunnerOutcome {
    return RunnerOutcome.Failure(
      message = message,
      exit = exitCode,
      cause = cause,
    )
  }

  /**
   * Emit a successful outcome for the current job.
   *
   * @return A [RunnerOutcome] representing a successful execution.
   */
  protected fun success(): RunnerOutcome = RunnerOutcome.Success

  /**
   * ### Invoke with Job
   *
   * Invokes this runner with the given [exec] job, prepared by the internals of this class from a [RunnerJob] instance
   * of type [T]. The resulting job is a [Deferred] instance that yields a [RunnerOutcome] when completed.
   *
   * @param exec The [RunnerExecution] to invoke this runner with.
   * @return A [Deferred] instance that yields a [RunnerOutcome].
   */
  protected abstract suspend operator fun invoke(exec: RunnerExecution<T>): RunnerOutcome
}
