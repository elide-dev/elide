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

/**
 * ## Runner Execution
 *
 * Internal state class which models available facilities and inputs for a [RunnerJob] as it executes within the context
 * of a [Runner].
 */
public interface RunnerExecution<T> where T: RunnerJob {
  /**
   * The [RunnerJob] which is under execution.
   */
  public val job: T

  /**
   * The Truffle [Context] in which this job is executing, as applicable.
   */
  public val context: Context
}
