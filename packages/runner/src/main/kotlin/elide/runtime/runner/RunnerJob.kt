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

import elide.tooling.Arguments
import elide.tooling.Inputs
import elide.tooling.Outputs

/**
 * ## Runner Job
 *
 * Describes a sealed hierarchy base of job types which can be executed by a given [Runner]. Runners orchestrate the
 * execution of guest code; each execution of guest code is represented by a [RunnerJob].
 */
public sealed interface RunnerJob {
  /**
   * ### Inputs
   *
   * Specifies typed inputs which relate to this runner job.
   */
  public val inputs: Inputs

  /**
   * ### Outputs
   *
   * Specifies typed outputs which relate to this runner job.
   */
  public val outputs: Outputs

  /**
   * ### Arguments
   *
   * Specifies arguments to use for this runner job.
   */
  public val arguments: Arguments

  /**
   * ### Runner Job (Sources)
   *
   * Represents a runner job which executes guest code from sources. This is typically used for languages which are
   * interpreted, or which are compiled before execution.
   */
  public sealed interface RunSources : RunnerJob

  /**
   * ### Runner Job (Bytecode)
   *
   * Represents a runner job which executes guest code from bytecode. This is typically used for languages which are
   * already compiled by the time they are executed.
   */
  public sealed interface RunBytecode : RunnerJob

  /**
   * ### Runner Job (Native)
   *
   * Represents a runner job which executes guest code from native binaries. This is typically used for languages which
   * compile directly to native code.
   */
  public sealed interface RunNative : RunnerJob
}
