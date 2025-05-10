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
package elide.exec

import kotlinx.atomicfu.atomic

/**
 * # Tracing
 *
 * Implements native execution tracing integration, via the `trace` crate; this object provides the native surface
 * corresponding to that library. Tracing is used throughout Elide's native code to provide a consistent source of truth
 * for logging and program behavior.
 *
 * ## Usage
 *
 * To initialize the tracing layer, call [ensureLoaded]; this method is idempotent and multiple calls are no-ops. If an
 * error surfaces while loading the native library, it is thrown immediately as an [IllegalStateException]. After the
 * trace layer has initialized, native calls on this object can occur directly.
 *
 * @see TraceChannel Trace channels
 * @see TraceSubscriber Trace subscribers
 * @see Execution Execution utilities
 */
public object Tracing {
  private const val LIB_NAME = "trace"

  // Whether the native tools have initialized.
  private val initialized = atomic(false)

  // Load the native trace library.
  private fun loadNative() {
    try {
      System.loadLibrary(LIB_NAME)
    } catch (err: UnsatisfiedLinkError) {
      throw IllegalStateException("Failed to load 'lib$LIB_NAME' native code", err)
    }
  }

  init {
    ensureLoaded()
  }

  /**
   * Ensure native tracing facilities have loaded.
   */
  public fun ensureLoaded() {
    if (!initialized.value) {
      synchronized(this) {
        loadNative()
        initialize().also {
          check(it == 0) { "Failed to initialize native trace layer: code $it" }
          initialized.compareAndSet(false, true)
        }
        // add a shutdown hook to clean up the tracing layer; this also flushes any final events
        Runtime.getRuntime().addShutdownHook(Thread {
          flush()
          shutdown()
          initialized.compareAndSet(true, false)
        })
      }
    }
  }

  /**
   * Initialize the native trace layer.
   *
   * @return Integer indicating success or failure; `0` indicates success.
   */
  @JvmName("initialize") private external fun initialize(): Int

  /**
   * Trigger a flush of all native events.
   *
   * @return Integer indicating success or failure; `0` indicates success.
   */
  @JvmName("flush") private external fun flush(): Int

  /**
   * Shutdown and clean up the native tracing layer.
   */
  @JvmName("shutdown") private external fun shutdown()

  /**
   * Trigger a native log to be delivered back to the JVM; this is mostly for testing.
   *
   * @param level Severity/level parameter for the log message.
   * @param message String message to enclose in the log message.
   * @return `0` if delivered, an error code if not.
   */
  @JvmName("nativeLog") public external fun nativeLog(level: String, message: String): Int

  /**
   * Trigger a native trace event to be delivered back to the JVM; this is mostly for testing.
   *
   * @param level Severity/level parameter for the trace message.
   * @param message String message to enclose in the trace message.
   * @return `0` if delivered, an error code if not.
   */
  @JvmName("nativeTrace") public external fun nativeTrace(level: String, message: String): Int
}
