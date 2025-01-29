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
package elide.runtime.intrinsics.js.node.childProcess

import elide.annotations.API
import elide.runtime.node.childProcess.ProcessInputStream
import elide.runtime.node.childProcess.ProcessOutputStream

/**
 * # Process I/O Channels
 *
 * Holds reference to `stdio`, `stderr`, and `stdin`, for a given foreign child process.
 */
@API public interface ProcessIOChannels {
  /**
   * Standard-in channel attached to the process, if available.
   */
  public val stdin: ProcessInputStream?

  /**
   * Standard-out channel attached to the process.
   */
  public val stdout: ProcessOutputStream?

  /**
   * Standard-error channel attached to the process.
   */
  public val stderr: ProcessOutputStream?
}
