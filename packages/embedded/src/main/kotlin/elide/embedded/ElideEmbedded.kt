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

package elide.embedded

import java.util.concurrent.CompletionStage
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.coroutines.future.asCompletableFuture
import elide.embedded.ElideEmbedded.State.*
import elide.embedded.ElideEmbedded.State.Uninitialized.context
import elide.embedded.internal.MicronautRuntimeContext
import elide.runtime.Logging

/**
 * A [CompletionStage] returned by runtime and application lifecycle operations, meant to be used by Java code to
 * observe the progress of coroutine-based features.
 */
public typealias LifecycleStage = CompletionStage<Unit>

/**
 * An entrypoint for the Elide Embedded Runtime, providing lifecycle managment for a single runtime instance.
 *
 * Using a singleton [ElideEmbedded] instance is the recommended usage, as this will prevent unnecessary resource
 * allocation. A single instance can dispatch multiple concurrent calls for separate guest applications.
 *
 * ## Lifecycle
 *
 * Before any operations can be performed, the runtime must be [initialized][initialize], specifying the
 * [configuration][EmbeddedConfiguration] to be used, which defines the format and version of the protocol used for
 * serialized invocations and other data structures exchanged with the host application.
 *
 * It is not allowed to initialize the runtime more than once, [initialize] calls will return `false` after the first
 * succesful invocation.
 *
 * ### Registration
 *
 * Guest applications must be [registered][createApp] with the runtime prior to call dispatch. A single application
 * must not be registered more than once. It is possible to register new applications after [starting][start] the
 * runtime.
 *
 * Registration causes the runtime to allocate the necessary resources for the guest application. The entrypoint and
 * configuration for the app are also evaluated at this stage in preparation for dispatch.
 *
 * ### Starting
 *
 * Once the runtime has been initialized and the initial guest applications are registered, the runtime can be
 * [started][start], which will enable the use of [dispatch] for handling incoming calls.
 *
 * ### Stopping
 *
 * The runtime can be [stopped][stop] at any time after starting, rejecting any new [dispatch] calls, but allowing
 * ongoing requests to complete execution. Once all pending calls have been processed, the runtime will shut down,
 * releasing guest-related resources.
 */
public class ElideEmbedded {
  /** Represents a stage in the runtime lifecycle. */
  private sealed interface State {
    /** The context associated with the runtime in the current state. */
    val context: EmbeddedRuntimeContext

    /**
     * Represents an uninitialized state for the runtime. The [context] is not available in this state and accessing
     * it will throw an exception.
     */
    data object Uninitialized : State {
      override val context: EmbeddedRuntimeContext
        get() = error("Context unavailable: runtime is not initialized")
    }

    /** Represents an initialized and configured state, before the runtime is started. */
    @JvmInline value class Initialized(override val context: EmbeddedRuntimeContext) : State

    /** Represents a started runtime state, capable of running guest applications and dispatching calls. */
    @JvmInline value class Running(override val context: EmbeddedRuntimeContext) : State

    /** Represents a shut down runtime, unable to run applications, dispatch calls, or start again. */
    @JvmInline value class Stopped(override val context: EmbeddedRuntimeContext) : State
  }

  /** Logger used by the embedded runtime. */
  private val logging by lazy { Logging.of(ElideEmbedded::class) }

  /** Current state of the runtime. */
  private val state = atomic<State>(Uninitialized)

  /** Use the [EmbeddedRuntimeContext] for the current [state]. */
  private inline fun <R> useContext(block: EmbeddedRuntimeContext.() -> R): R {
    return block(state.value.context)
  }

  private inline fun updateState(require: (State) -> Boolean, update: (State) -> State): Boolean {
    val original = state.getAndUpdate { current ->
      if (require(current)) update(current)
      else current
    }

    return require(original)
  }

  /**
   * Initialize the runtime and configure it, enabling registration of guest applications and preparing for a [start]
   * call. The [config] defines the format and version of the invocation protocol.
   *
   * Multiple [initialize] calls are not supported, instead all invocations following the first one will return `false`
   * to indicate failure without throwing an exception. There is no guarantee that this condition will be met for
   * concurrent initialization calls, runtime lifecycle methods should never be called concurrently.
   *
   * @param config Configuration for the runtime, including the dispatch protocol settings.
   * @return Whether the runtime was successfully initialized.
   */
  public fun initialize(config: EmbeddedConfiguration): Boolean {
    return updateState(require = { it is Uninitialized }) {
      logging.info("Initializing runtime")

      // select the context implementation manually (since DI becomes available only after init)
      // currently, only a Micronaut-based context is implemented
      Initialized(MicronautRuntimeContext.create(config))
    }
  }

  /**
   * Start the runtime, enabling [dispatch] calls and application lifecycle operations. Multiple [start] calls are not
   * supported and will have no effect.
   */
  public fun start(): Boolean {
    return updateState(require = { it !is Running }) { current ->
      when (current) {
        // start required
        is Initialized -> {
          logging.debug("Starting runtime")
          Running(current.context)
        }

        // illegal state
        is Stopped -> error("Runtime has been stopped and cannot be restarted")
        is Uninitialized -> error("Runtime must be initialized before starting")
        else -> error("Fatal error, runtime should not be in $current state")
      }
    }
  }

  /**
   * Stop the runtime, rejecting any new [dispatch] calls. Calling [start] after stopping the runtime is not allowed
   * and will have no effect.
   */
  public fun stop(): Boolean {
    return updateState(require = { it !is Stopped }) { current ->
      when (current) {
        // shutdown required
        is Running -> {
          logging.debug("Stopping runtime")
          current.context.appRegistry.cancel()

          logging.debug("App registry closed, shutting down")
          Stopped(current.context)
        }

        is Uninitialized, is Initialized -> error("Runtime must be running before being stopped")
        else -> error("Fatal error, runtime should not be in $current state")
      }
    }
  }

  /**
   * Dispatch an incoming call with the runtime, returning a future to receive the response when ready. The [call]
   * parameter is inherently type-unsafe to accommodate for the differences in dispatch protocols; the runtime codec
   * will enforce type/format restrictions during deserialization instead.
   *
   * The returned response will follow the same type-safety rules as the call, and its form will depend on the protocol
   * implementation selected during rungime initialization.
   *
   * The selected [app] must be running for this operation to succeed.
   */
  public fun dispatch(call: UnsafeCall, app: EmbeddedApp): CompletionStage<UnsafeResponse> {
    return useContext {
      dispatcher.dispatch(codec.decode(call), app).asCompletableFuture().thenApply(codec::encode)
    }
  }

  /**
   * Crate and register a guest application with the runtime using the provided [id] and initial [config]. The returned
   * reference can be used to manage the app's lifecycle and observe its state.
   */
  public fun createApp(id: String, config: EmbeddedAppConfiguration): EmbeddedApp {
    return useContext {
      logging.debug("Registering application with id '$id'")
      appRegistry.register(EmbeddedAppId(id), config)
    }
  }

  /**
   * Start an embedded [app], invoking a callback once the operation completes, with an indication of success. This
   * method is intended for the Java/Native interoperability layer, which has no direct support for coroutines.
   *
   * Note that the application will not start immediately, instead startup will be scheduled if possible. The callback
   * will be called with `true` as an argument as long as no exceptions are thrown, regardless of whether the app was
   * started or not.
   */
  public fun startApp(app: EmbeddedApp): LifecycleStage {
    check(state.value is Running) { "Runtime must be started before launching an application" }

    logging.debug("Starting app '${app.id}'")
    return app.start().asCompletableFuture()
  }

  /**
   * Stop an embedded [app]. An observable version of this method providing callback capabilities is also available
   * This method is intended for the Java/Native interoperability layer, which has no direct support for coroutines.
   *
   * Note that the application will not stop immediately, instead shutdown will be scheduled if possible. The function
   * will return normally regardless of whether the app was stopped or not.
   */
  public fun stopApp(app: EmbeddedApp): LifecycleStage {
    check(state.value is Running) { "Runtime is not running, unable to directly stop application" }

    logging.debug("Stopping app '${app.id}'")
    return app.stop().asCompletableFuture()
  }
}
