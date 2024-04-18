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

package elide.runtime.gvm.internals

import java.util.concurrent.atomic.AtomicReference

/**
 * # Process Manager
 *
 * Initialized early in Elide's startup routine, the Process Manager keeps track of data which is entirely static in
 * nature, and which must be shared across many different parts of the runtime. For example, the arguments the program
 * was dispatched with, and the current working directory, and so on.
 *
 * When the runtime starts up, the Process Manager is initialized in static form with these values. Under testing, DI is
 * used to mock these values.
 */
public interface ProcessManager {
  /**
   * ## Arguments
   *
   * Retrieve the arguments the program was dispatched with.
   *
   * @return Array of arguments
   */
  public fun arguments(): Array<String>

  /**
   * ## Working Directory
   *
   * Retrieve the current working directory of the program.
   *
   * @return Current working directory
   */
  public fun workingDirectory(): String

  /** Static access to process manager state. */
  public companion object {
    private val initialized: AtomicReference<Boolean> = AtomicReference(false)
    private val args: AtomicReference<Array<String>> = AtomicReference(emptyArray())
    private val workingDir: AtomicReference<String> = AtomicReference("")
    private val facade = object : ProcessManager {
      override fun arguments(): Array<String> {
        return args.get()
      }

      override fun workingDirectory(): String {
        return workingDir.get()
      }
    }

    /**
     * ## Initialize State
     *
     * Initialize process manager state. This entrypoint should ONLY be used from actual entrypoints; DO NOT call this
     * method under test circumstances.
     */
    @JvmStatic public fun initializeStatic(arguments: Array<String>, workingDirectory: String) {
      require(initialized.compareAndSet(false, true)) { "Process manager already initialized" }
      args.set(arguments)
      workingDir.set(workingDirectory)
    }

    /**
     * ## Acquire
     *
     * Acquire a process manager instance.
     *
     * @return Process manager instance
     */
    @JvmStatic public fun acquire(): ProcessManager = facade
  }
}
