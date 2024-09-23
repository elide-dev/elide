/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js.node.childProcess

import elide.annotations.API
import elide.runtime.gvm.internals.node.childProcess.ProcessOutputStream
import elide.runtime.intrinsics.js.err.JsError
import elide.runtime.intrinsics.js.node.process.ProcessStandardOutputStream
import elide.vm.annotations.Polyglot

/**
 * # Node: Child Process (Synchronous)
 *
 * Describes the API made available for a synchronously spawned child process handle; objects of this type are typically
 * returned from Node Process API methods like `spawnSync`.
 */
@API public interface ChildProcessSync {
  /**
   * ## Process ID
   *
   * Returns the process ID for the child process.
   */
  @get:Polyglot public val pid: Long

  /**
   * ## Output
   *
   * Lazily calculated array of results from `stdio` output.
   */
  @get:Polyglot public val output: LazyProcessOutput?

  /**
   * ## Standard Out
   *
   * Standard output stream for the child process.
   */
  @get:Polyglot public val stdout: ProcessOutputStream?

  /**
   * ## Standard Error
   *
   * Standard error output stream for the child process.
   */
  @get:Polyglot public val stderr: ProcessOutputStream?

  /**
   * ## Terminal Status
   *
   * Exit code for the process, or `null` if the subprocess terminated due to a signal.
   */
  @get:Polyglot public val status: Int?

  /**
   * ## Terminal Signal
   *
   * The signal used to kill the subprocess, or `null` if the process did not terminate due to a signal.
   */
  @get:Polyglot public val signal: String?

  /**
   * ## Terminal Error
   *
   * The error object if the child process failed or timed out.
   */
  @get:Polyglot public val error: JsError?
}
