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

@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package elide.exec

import kotlinx.atomicfu.atomic
import elide.exec.Coordinator.Default.satisfy
import elide.exec.Coordinator.Default.satisfyFor
import elide.runtime.Logging

/**
 * # Execution Engine
 *
 * Implements the JVM surface of the `exec` crate, which is responsible for managing a Tokio engine that is used for
 * multithreaded and async execution within native contexts.
 *
 * The execution engine is also integrated with the `trace` crate, which provides trace subscribes that bridge back to
 * JVM for event delivery.
 *
 * ## Usage
 *
 * Load the native `exec` library and initialize the layer by calling [ensureLoaded]; this method is idempotent and will
 * throw if errors occur. After this point, native methods on [Execution] or [Tracing] can be used directly.
 *
 * @see Tracing Execution tracing support
 */
public object Execution {
  private const val SUBSTRATE_LIB_NAME = "substrate"
  private const val EXEC_LIB_NAME = "exec"

  // Whether the native executor has initialized.
  private val initialized = atomic(false)
  private fun isInitialized(): Boolean = initialized.value
  private val logger by lazy { Logging.of(Execution::class) }

  // Listener for VM shutdown.
  @Suppress("TooGenericExceptionCaught")
  private val shutdownHookResponder = object : Thread("elide-exec-shutdown") {
    override fun run() {
      if (isInitialized()) {
        try {
          shutdown()
        } catch (err: Throwable) {
          // Ignore errors during shutdown.
          logger.warn(
            "Error thrown during async executor shutdown",
            err,
          )
        } finally {
          initialized.value = false
        }
      }
    }
  }

  // Load the native executor library.
  private fun loadNative(name: String) {
    try {
      System.loadLibrary(name)
    } catch (err: UnsatisfiedLinkError) {
      throw IllegalStateException("Failed to load 'lib$name' native code", err)
    }
  }

  init {
    ensureLoaded()
  }

  /**
   * Ensure native execution facilities have loaded.
   */
  public fun ensureLoaded() {
    if (!initialized.value) {
      synchronized(this) {
        // exec depends on tracing, which needs to load first
        Tracing.ensureLoaded()
        loadNative(SUBSTRATE_LIB_NAME)
        loadNative(EXEC_LIB_NAME)

        initialize().also {
          check(it == 0) { "Failed to initialize native executor layer: code $it" }
          initialized.compareAndSet(false, true)
          Runtime.getRuntime().addShutdownHook(shutdownHookResponder)
        }
      }
    }
  }

  /**
   * Initialize the native executor layer.
   *
   * @return Integer indicating success or failure; `0` indicates success.
   */
  private external fun initialize(): Int

  /**
   * Shut down the native executor layer.
   *
   * @return Integer indicating success or failure; `0` indicates success.
   */
  private external fun shutdown(): Int

  /**
   * ## Execution Coordinator
   */
  @Suppress("UNUSED_PARAMETER")
  public fun coordinator(options: Coordinator.Options): Coordinator {
    TODO("")
  }
}

/**
 * Execute a [TaskGraph] within a new default [Coordinator]; this yields a [TaskGraphExecution] handle, which can be
 * used to subscribe to execution events and control execution flow.
 *
 * @return [TaskGraphExecution] handle for the execution
 */
public suspend fun TaskGraph.execute(
  scope: ActionScope,
  coordinator: Coordinator,
  binder: ExecutionBinder? = null,
): TaskGraphExecution = coordinator.satisfy(scope, this, binder)

/**
 * Execute a specific [taskId] within the context of a built [TaskGraph].
 *
 * @return [TaskGraphExecution] handle for the execution
 */
public suspend fun TaskGraph.execute(
  scope: ActionScope,
  taskId: TaskId,
  binder: ExecutionBinder? = null,
): TaskGraphExecution = satisfyFor(scope, this, taskId, binder)

/**
 * Execute a [TaskGraph] within a new default [Coordinator]; this yields a [TaskGraphExecution] handle, which can be
 * used to subscribe to execution events and control execution flow.
 *
 * @return [TaskGraphExecution] handle for the execution
 */
public suspend fun TaskGraph.execute(scope: ActionScope, binder: ExecutionBinder? = null): TaskGraphExecution.Listener =
  satisfy(scope, this, binder)
