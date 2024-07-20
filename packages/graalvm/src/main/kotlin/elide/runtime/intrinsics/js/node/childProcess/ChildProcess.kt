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
import elide.runtime.gvm.internals.node.childProcess.ProcessInputStream
import elide.runtime.gvm.internals.node.childProcess.ProcessOutputStream
import elide.runtime.intrinsics.js.node.events.EventEmitter
import elide.runtime.intrinsics.js.node.events.EventTarget

/**
 * # Child Process
 *
 * Describes the API made available for a child process handle; objects of this type are typically returned from Node
 * Process API methods like `spawn`, which give the caller direct control over the spawned subprocess.
 */
@API public interface ChildProcess : EventTarget, EventEmitter, ProcessChannel {
  /**
   * ## Process ID
   *
   * Returns the process ID for the child process.
   */
  public val pid: Long

  /**
   * ## Connected
   *
   * Whether the subprocess is active and connected.
   */
  public val connected: Boolean

  /**
   * ## Channel
   *
   * Channel used to communicate with the underlying process.
   */
  public val channel: ProcessChannel

  /**
   * ## Standard Input
   *
   * Standard input stream for the child process.
   */
  public val stdin: ProcessInputStream?

  /**
   * ## Standard Output
   *
   * Standard output stream for the child process.
   */
  public val stdout: ProcessOutputStream?

  /**
   * ## Standard Error
   *
   * Standard error output stream for the child process.
   */
  public val stderr: ProcessOutputStream?

  /**
   * ## Standard Error
   *
   * Standard error output stream for the child process.
   */
  public val stdio: ProcessIOChannels

  /**
   * ## Exit Code
   *
   * Exit code for the process, or `null` if the subprocess terminated due to a signal.
   */
  public val exitCode: Int?

  /**
   * ## Signal Code
   *
   * Terminal signal code for the process, or `null` if the subprocess terminated normally.
   */
  public val signalCode: String?

  /**
   * ## Killed
   *
   * Whether the process terminated by being killed.
   */
  public val killed: Boolean

  /**
   * ## Kill
   *
   * Kills the child process, using the provided signal.
   *
   * @param signal Signal to use to kill the process.
   */
  public fun kill(signal: String)

  /**
   * ## Disconnect
   *
   * Disconnects the child process from the parent.
   */
  public fun disconnect()

  /**
   * ## Wait
   *
   * Non-standard method which waits for process exit.
   */
  public fun wait(): Int
}
