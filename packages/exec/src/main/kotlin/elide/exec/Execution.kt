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
 */
public object Execution {
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
  private fun loadNative() {
    try {
      System.loadLibrary("exec")
    } catch (err: UnsatisfiedLinkError) {
      throw IllegalStateException("Failed to load 'libexec' native code", err)
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
        loadNative()
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
