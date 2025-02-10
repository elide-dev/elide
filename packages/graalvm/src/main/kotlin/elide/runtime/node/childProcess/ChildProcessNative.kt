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
package elide.runtime.node.childProcess

/**
 * # Child Process: Native Utilities
 *
 * Maps JNI-provided methods for native (primitive) operations on child processes; support for these operations may vary
 * by operating system.
 */
internal object ChildProcessNative {
  init {
    try {
      System.loadLibrary("umbrella")
    } catch (err: UnsatisfiedLinkError) {
      try {
        System.loadLibrary("posix")
      } catch (err: UnsatisfiedLinkError) {
        throw IllegalStateException("Failed to load child process tools", err)
      }
    }
  }

  /**
   * ## Kill with Signal
   *
   * Sends a signal to the process with the given PID; the return value is either `0`, indicating a successful signal
   * send, or non-zero, indicating an error.
   *
   * @param pid The process ID to send the signal to.
   * @param signal The signal to send to the process.
   * @return The return value of the signal operation.
   */
  @JvmStatic @JvmName("killWith") internal external fun killWith(pid: Long, signal: String): Int

  /**
   * ## Current Process Priority
   *
   * Retrieve the priority value set for the current process, if supported. If the operation is not supported, the value
   * `0` is returned as a default.
   *
   * @return The priority value of the current process.
   */
  @JvmStatic @JvmName("currentProcessPriority") internal external fun currentProcessPriority(): Int

  /**
   * ## Set Current Process Priority
   *
   * Set the priority value set for the current process, if supported; if the operation is not supported, the value `-1`
   * is returned.
   *
   * @param priority The priority value to set for the current process.
   * @return The priority value of the current process; either the value provided, or `-1` if the operation is not
   *   supported.
   */
  @JvmStatic @JvmName("currentProcessPriority") internal external fun setCurrentProcessPriority(priority: Int): Int

  /**
   * ## Get Process Priority
   *
   * Retrieve the priority value set for the specified process, addressed by its [pid], if supported. If the operation
   * is not supported, the value `0` is returned as a default.
   *
   * @param pid The process ID to retrieve priority for.
   * @return The priority value of the specified process.
   */
  @JvmStatic @JvmName("currentProcessPriority") internal external fun getProcessPriority(pid: Long): Int

  /**
   * ## Set Process Priority
   *
   * Set the [priority] value set for the specified process, addressed by its [pid], if supported; if the operation is
   * not supported, the value `-1` is returned.
   *
   * @param pid The process ID to set priority for.
   * @param priority The priority value to set for the current process.
   * @return The priority value of the specified process; either the value provided, or `-1` if the operation is not
   *   supported.
   */
  @JvmStatic @JvmName("setProcessPriority") internal external fun setProcessPriority(pid: Long, priority: Int): Int
}
