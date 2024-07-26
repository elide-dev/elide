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
package elide.runtime.gvm.internals.node.childProcess

/**
 * # Child Process: Native Utilities
 *
 * Maps JNI-provided methods for native (primitive) operations on child processes; support for these operations may vary
 * by operating system.
 */
internal object ChildProcessNative {
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
  @JvmStatic @JvmName("killWith") internal external fun killWith(pid: Int, signal: String): Int
}
