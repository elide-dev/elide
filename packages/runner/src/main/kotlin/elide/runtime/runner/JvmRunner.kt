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

import java.util.Optional
import elide.tooling.Arguments
import elide.tooling.Classpath
import elide.tooling.Environment
import elide.tooling.Modulepath

/**
 * ## JVM Runner
 *
 * Extends the base concept of a [Runner.BytecodeRunner] for use as a JVM-specific runner; gathers JVM-style arguments
 * and inputs and then executes them in a given JVM-like environment.
 */
public interface JvmRunner : Runner.BytecodeRunner<JvmRunner.JvmRunnerJob> {
  /**
   * ### JVM Runner Job
   *
   * @property mainClass Main class to execute.
   * @property mainModule Optional main module within which the main class resides, if applicable.
   * @property classpath Classpath to use for the job.
   * @property modulepath Optional modulepath to use for the job, if applicable.
   * @property environment Optional environment to use for the job, if applicable.
   * @property jvmArgs JVM arguments to use for the job, if applicable.
   * @param args Arguments to pass to the job.
   */
  public class JvmRunnerJob internal constructor (
    public val mainClass: String,
    public val mainModule: Optional<String>,
    public val classpath: Classpath,
    public val modulepath: Modulepath? = null,
    public val environment: Environment? = null,
    public val jvmArgs: Arguments = Arguments.empty(),
    args: Arguments,
  ) : AbstractRunnerJob(args), RunnerJob.RunBytecode

  /** Factories for [JvmRunnerJob] instances. */
  public companion object {
    /**
     * Create a classpath-based JVM runner job.
     *
     * This method supports a minimal suite of arguments; for full arguments, use [JvmRunner.create].
     *
     * @param mainClass Main class to execute.
     * @param classpath Classpath to use for the job.
     * @param args Arguments to pass to the job.
     * @param env Optional environment to use for the job, if applicable.
     *
     * @return A new [JvmRunnerJob] instance configured with the provided parameters.
     */
    @JvmStatic public fun of(
      mainClass: String,
      classpath: Classpath,
      args: Arguments,
      env: Environment? = null,
    ): JvmRunnerJob = JvmRunnerJob(
      mainClass = mainClass,
      mainModule = Optional.empty(),
      classpath = classpath,
      modulepath = null,
      environment = env,
      args = args,
    )

    /**
     * Create a classpath-based JVM runner job, potentially with module support.
     *
     * @param main Main module and class to execute, as a pair of (module, class).
     * @param classpath Classpath to use for the job.
     * @param modulepath Modulepath to use for the job.
     * @param args Arguments to pass to the job.
     * @param env Optional environment to use for the job, if applicable.
     *
     * @return A new [JvmRunnerJob] instance configured with the provided parameters.
     */
    @JvmStatic public fun create(
      main: Pair<String?, String>,
      classpath: Classpath,
      modulepath: Modulepath,
      args: Arguments,
      env: Environment? = null,
    ): JvmRunnerJob = JvmRunnerJob(
      mainClass = main.second,
      mainModule = Optional.ofNullable(main.first),
      classpath = classpath,
      modulepath = modulepath,
      environment = env,
      args = args,
    )
  }
}
