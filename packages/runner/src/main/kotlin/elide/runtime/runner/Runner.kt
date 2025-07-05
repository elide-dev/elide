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
import java.util.function.Consumer
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext

/**
 * # Runner
 *
 * Sealed type hierarchy base for all runners, in all languages or contexts. Runners are responsible for executing guest
 * code through Elide's runtime facilities.
 *
 * @property info Information about this runner.
 */
public sealed interface Runner<T> : AutoCloseable, Consumer<T> where T: RunnerJob {
  /**
   * Provide basic information about this runner; this includes the runner's name, version, or other uniform metadata
   * available for all runners.
   */
  public val info: RunnerInfo

  /**
   * Assign a Truffle [context] to use for this runner. This method must be called at least once before invoking the
   * runner with a job, if the runner plans to use Truffle execution facilities.
   *
   * @param context The [Context] to configure this runner with.
   */
  public fun configure(context: Context, coroutineContext: CoroutineContext)

  /**
   * Poll for an outcome from the runner, if a current job is active; this method returns the [Deferred] job that is
   * currently executing, or `null` if no job is active.
   *
   * @return A [Deferred] instance that is currently under execution, or `null` if no job is active.
   */
  public fun poll(): Deferred<RunnerOutcome>?

  /**
   * Await the final outcome of the current job, if one is active. This method suspends until the job completes and
   * returns the [RunnerOutcome] of the job.
   *
   * Note that this method will throw an exception if no job is currently active, so it should only be called when
   * [poll] returns a non-null value, or when the runner is known to have an active job.
   *
   * @return The [RunnerOutcome] of the currently active job.
   */
  public suspend fun await(): RunnerOutcome

  /**
   * All in one cycle, mount and initialize a new job within the runner, then execute it, then await a result, and
   * provide it as a return value to the caller.
   *
   * @param job The [RunnerJob] to execute.
   * @return A [Deferred] instance that will complete with the [RunnerOutcome] of the job.
   */
  public suspend operator fun invoke(job: T): RunnerOutcome

  // --- Runner Types

  /**
   * ## Runner (Source)
   *
   * Expects to run guest code in the form of source code, which will be interpreted for execution (or, alternatively,
   * compiled on-the-fly).
   */
  public sealed interface SourceRunner<T> : Runner<T> where T: RunnerJob.RunSources

  /**
   * ## Runner (Bytecode)
   *
   * Expects to run guest code in the form of bytecode, which will be executed by a virtual machine or interpreter.
   */
  public sealed interface BytecodeRunner<T> : Runner<T> where T: RunnerJob.RunBytecode

  /**
   * ## Runner (Native)
   *
   * Expects to run guest code in the form of native code, which will be executed directly.
   */
  public sealed interface NativeRunner<T> : Runner<T> where T: RunnerJob.RunNative
}
