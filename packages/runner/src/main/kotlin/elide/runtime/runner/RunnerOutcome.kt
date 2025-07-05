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

/**
 * ## Runner Outcome
 *
 * Describes typed outcomes which can yield from a [RunnerJob] execution by a [Runner]. Outcomes for runners are simply
 * modeled after program exit codes, since runners typically execute guest programs and return those details.
 */
public sealed interface RunnerOutcome {
  /**
   * Whether this outcome represents a successful execution.
   */
  public val isSuccess: Boolean

  /**
   * Exit code to use for this outcome.
   */
  public val exit: Int

  /**
   * Successful runner outcome.
   */
  public data object Success : RunnerOutcome {
    override val isSuccess: Boolean get() = true
    override val exit: Int get() = 0
  }

  /**
   * Failed runner outcome.
   *
   * @property exit Exit code for this outcome.
   * @property message Optional message for this outcome, if any.
   */
  @JvmRecord public data class Failure(
    override val exit: Int,
    val message: String? = null,
    val cause: Throwable? = null,
  ) : RunnerOutcome {
    override val isSuccess: Boolean get() = false
  }
}
