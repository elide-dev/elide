package elide.embedded

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import elide.embedded.internal.MicronautRuntimeContext
import elide.runtime.Logging

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
  /** Logger used by the embedded runtime. */
  private val logging = Logging.of(ElideEmbedded::class)

  /** Shared context for dependency injection and runtime lifecycle management. */
  private val context = atomic<EmbeddedRuntimeContext?>(null)

  /** A coroutine scope used to launch listeners, allowing Java and native code to use coroutines. */
  private val scope = atomic<CoroutineScope?>(null)

  /** Thread-safe flag to avoid duplicate initialization. */
  private val initialized = atomic(false)

  /** Use the value of this atomic reference or throw an exception if the value has not been initialized yet. */
  private fun <T> AtomicRef<T?>.getOrFail(): T {
    return value ?: error("Uninitialized")
  }

  /**
   * Initialize the runtime and configure it, enabling registration of guest applications and preparing for a [start]
   * call. The [config] defines the format and version of the invocation protocol.
   *
   * Multiple [initialize] calls are not supported, instead all invocations following the first one will return `false`
   * to indicate failure without throwing an exception.
   *
   * @param config Configuration for the runtime, including the dispatch protocol settings.
   * @return Whether the runtime was successfully initialized.
   */
  public fun initialize(config: EmbeddedConfiguration): Boolean {
    // runtime config serves as initialization flag
    if (!initialized.compareAndSet(expect = false, update = true)) return false
    logging.info("Initializing runtime")

    // select the context implementation manually (since DI becomes available only after init)
    // currently, only a Micronaut-based context is implemented
    context.value = MicronautRuntimeContext.create(config)

    logging.debug("Initialized runtime with configuration $config and context ${context.value}")
    return true
  }

  /**
   * Start the runtime, enabling [dispatch] calls and application lifecycle operations. Multiple [start] calls are not
   * supported and will have no effect.
   */
  public fun start(): Boolean {
    if (scope.value != null) return false
    logging.debug("Starting runtime")

    // prepare the adapter scope for app lifecycle operations
    scope.value = CoroutineScope(Dispatchers.IO + SupervisorJob())

    return true
  }

  /**
   * Stop the runtime, rejecting any new [dispatch] calls. Calling [start] after stopping the runtime is not allowed
   * and will have no effect.
   */
  public fun stop() {
    logging.debug("Stopping runtime")

    // shut down the registry, stop all running apps, and cancel all observers
    context.getOrFail().appRegistry.cancel()
    scope.getOrFail().cancel()

    logging.debug("App registry and observers scope closed, shutting down")
  }

  /**
   * Dispatch an incoming call with the runtime. This operation is currently under construction.
   */
  public fun dispatch() {
    logging.debug("Dispatching call")
    // nothing yet
  }

  /**
   * Crate and register a guest application with the runtime using the provided [id] and initial [config]. The returned
   * reference can be used to manage the app's lifecycle and observe its state.
   */
  public fun createApp(id: String, config: EmbeddedAppConfiguration): EmbeddedApp {
    logging.debug("Registering application with id '$id'")
    val app = context.getOrFail().appRegistry.register(EmbeddedAppId(id), config)
    logging.debug("Registered application with id '$id'")

    return app
  }

  /**
   * Start an embedded [app]. An observable version of this method providing callback capabilities is also available
   * This method is intended for the Java/Native interoperability layer, which has no direct support for coroutines.
   *
   * Note that the application will not start immediately, instead startup will be scheduled if possible. The function
   * will return normally regardless of whether the app was started or not.
   */
  public fun startApp(app: EmbeddedApp) {
    logging.debug("Starting app '${app.id}'")
    try {
      app.start()
      logging.debug("Started app '${app.id}'")
    } catch (cause: Throwable) {
      logging.error("Failed to start app '${app.id}': $cause")
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
  public fun startApp(app: EmbeddedApp, onComplete: (success: Boolean) -> Unit) {
    logging.debug("Starting app '${app.id}'")
    scope.getOrFail().launch {
      try {
        app.start().join()

        logging.debug("Started app '${app.id}'")
        onComplete(true)
      } catch (cause: Throwable) {
        logging.error("Failed to start app '${app.id}': $cause")
        onComplete(false)
      }
    }
  }

  /**
   * Stop an embedded [app]. An observable version of this method providing callback capabilities is also available
   * This method is intended for the Java/Native interoperability layer, which has no direct support for coroutines.
   *
   * Note that the application will not stop immediately, instead shutdown will be scheduled if possible. The function
   * will return normally regardless of whether the app was stopped or not.
   */
  public fun stopApp(app: EmbeddedApp) {
    logging.debug("Stopping app '${app.id}'")
    try {
      app.stop()
      logging.debug("Stopped app '${app.id}'")
    } catch (cause: Throwable) {
      logging.error("Failed to stop app '${app.id}': $cause")
    }
  }

  /**
   * Stop an embedded [app], invoking a callback once the operation completes, with an indication of success. This
   * method is intended for the Java/Native interoperability layer, which has no direct support for coroutines.
   *
   * Note that the application will not stop immediately, instead shutdown will be scheduled if possible. The callback
   * will be called with `true` as an argument as long as no exceptions are thrown, regardless of whether the app was
   * stopped or not.
   */
  public fun stopApp(app: EmbeddedApp, onComplete: (success: Boolean) -> Unit) {
    logging.debug("Stopping app '${app.id}'")
    scope.getOrFail().launch {
      try {
        app.stop().join()

        logging.debug("Stopped app '${app.id}'")
        onComplete(true)
      } catch (cause: Throwable) {
        logging.error("Failed to stop app '${app.id}': $cause")
        onComplete(false)
      }
    }
  }
}