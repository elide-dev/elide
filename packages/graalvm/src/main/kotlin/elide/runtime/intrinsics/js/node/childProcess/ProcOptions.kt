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

import kotlin.time.Duration
import elide.annotations.API

/**
 * ## Process Options
 *
 * Defines base options which relate to process launching through the Node Child Process API; these options are further
 * used/extended from concrete options types, such as [ExecSyncOptions].
 */
@API public sealed interface ProcOptions {
  /**
   * ## Current Working Directory (String)
   *
   * Provides the set current-working-directory for a process operation as a [String] value.
   */
  public val cwdString: String?

  /**
   * ## Timeout (Integer)
   *
   * Provides a timeout value for a process operation, in seconds.
   */
  public val timeout: Duration?

  /**
   * ## Shell
   *
   * Shell to wrap this command in, if applicable/desired.
   */
  public val shell: String?

  /**
   * ## Encoding
   *
   * Encoding string specified by the user, as their desired output stream encoding.
   */
  public val encoding: String?

  /**
   * ## Kill Signal
   *
   * Signal which is preferred for use when killing this process.
   */
  public val killSignal: String

  /**
   * ## Timeout (Integer)
   *
   * Timeout, in seconds, after which the process should no longer be allowed to live.
   */
  public val timeoutSeconds: Long?

  /**
   * ## Max Buffer
   *
   * Maximum buffer size for process outputs; a reasonable default is provided.
   */
  public val maxBuffer: Int?

  /**
   * ## Standard I/O Configuration
   *
   * Specifies configurations for I/O streams for a process operation.
   */
  public val stdio: StdioConfig

  /**
   * ## Environment Variables
   *
   * Provides a set of environment variables for a process operation; if not provided (`null`), the current environment
   * is used.
   *
   * An empty map can be provided to force an empty environment.
   */
  public val env: Map<String, String>?

  /**
   * ## Hide on Windows
   *
   * Whether to hide the process window for this process, if running on Windows; inert on all other platforms.
   */
  public val windowsHide: Boolean?
}
