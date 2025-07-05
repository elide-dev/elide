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

import kotlinx.atomicfu.atomic
import elide.tooling.Arguments
import elide.tooling.Inputs
import elide.tooling.Outputs

/**
 * ## Abstract Runner Job
 *
 * Describes the abstract base implementation of a [RunnerJob], regardless of type or underlying behavior; runner jobs
 * may collect inputs, outputs, arguments, and other environment information, and then are passed to [Runner] instances
 * for execution.
 */
public abstract class AbstractRunnerJob (args: Arguments? = null) : RunnerJob {
  private val currentInputs = atomic<Inputs?>(null)
  private val currentOutputs = atomic<Outputs?>(null)
  private val allArguments = atomic<Arguments>(args ?: Arguments.empty())

  /** Signifies no inputs. */
  public data object NoInputs : Inputs.None

  /** Signifies no outputs. */
  public data object NoOutputs : Outputs.None

  /**
   * Configure this runner job with the given inputs, outputs, and arguments.
   *
   * @param inputs Inputs to configure this job with, or `null` to signify no inputs.
   * @param outputs Outputs to configure this job with, or `null` to signify no outputs.
   * @param arguments Arguments to configure this job with, or an empty set of arguments.
   */
  public fun configure(
    inputs: Inputs? = null,
    outputs: Outputs? = null,
    arguments: Arguments = Arguments.empty(),
  ) {
    currentInputs.value = inputs ?: NoInputs
    currentOutputs.value = outputs ?: NoOutputs
    allArguments.value = arguments
  }

  override val inputs: Inputs get() = (currentInputs.value ?: NoInputs)
  override val outputs: Outputs get() = (currentOutputs.value ?: NoOutputs)
  override val arguments: Arguments get() = allArguments.value
}
