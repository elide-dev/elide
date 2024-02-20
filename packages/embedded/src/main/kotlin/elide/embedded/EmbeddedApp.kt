package elide.embedded

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import elide.embedded.EmbeddedAppConfiguration.DispatchMode.FETCH

/** A unique string-based identifier for a guest [EmbeddedApp]. */
@JvmInline public value class EmbeddedAppId(public val value: String)

/**
 * Represents a configuration for an embedded application, which can be used to resolve the entrypoint, language,
 * and resources associated with a guest app.
 *
 * Configurations are immutable, but an application's active configuration can be replaced after the initial value is
 * set.
 */
public data class EmbeddedAppConfiguration(
  /**
   * A path string, relative to the application root, pointing to a file containing the guest entrypoint. The file may
   * contain language sources (e.g. for JavaScript apps) or compiled code (JAR distributions for JVM apps).
   */
  val entrypoint: String,

  /**
   * A language code indicating the language of the application, used to select the dispatch engine and configure
   * app bindings.
   */
  val language: String,

  /**
   * Specifies the mode in which requests will be handled by the application. The selected value can greatly affect the
   * behavior of the dispatcher (e.g. certain modes require specific symbols to be exported by guest code).
   */
  val dispatchMode: DispatchMode,
) {
  /**
   * Describes the mode in which the runtime will dispatch requests with a guest application. Some modes require the
   * guest code to export specific symbols to be used as entrypoint, such as [FETCH].
   */
  public enum class DispatchMode {
    /**
     * Signals the runtime to use a JavaScript fetch-like API for dispatch, in which a function named `fetch` is
     * exported by the entrypoint script, and called with the request as an argument, using the return value as a
     * response for the call.
     *
     * For JavaScript applications, both the Request and Response types are compatible to the WinterCG specification.
     */
    FETCH
  }
}

/** Represents a stage in the lifecycle of a guest application. */
public enum class EmbeddedAppState {
  /** The application is ready to start. */
  READY,

  /** The application is starting but not yet processing requests. */
  STARTING,

  /** The application is running and accepting requests. */
  RUNNING,

  /** The application is stopping and no longer accepting requests, ongoing calls will be processed. */
  STOPPING,

  /** The application has stopped and will not accept requests until started again. */
  STOPPED,
}

/** Whether this state represents an idle application, i.e. [EmbeddedAppState.READY] or [EmbeddedAppState.STOPPED]. */
public val EmbeddedAppState.isIdle: Boolean
  get() = when (this) {
    EmbeddedAppState.READY, EmbeddedAppState.STOPPED -> true
    else -> false
  }


/**
 * An embedded guest application with lifecycle management.
 *
 * Applications behave as limited coroutine scopes via [dispatch], which allows scheduling suspending jobs while
 * the application is [running][EmbeddedAppState.RUNNING]. Stopping the application allows pending jobs to complete
 * while rejecting new dispatch calls.
 *
 * ### Lifecycle
 *
 * The application must be started by calling [start] before it can dispatch jobs. Once running, the active
 * configuration will be locked, requiring a restart to apply any changes.
 *
 * Use [stop] to reject new [dispatch] calls while allowing pending jobs to complete. The application can be started
 * and stopped as many times as necessary.
 *
 * ### Cancellation
 *
 * An application can be [cancelled][cancel], shutting down its internal scope and disabling [start] calls. During
 * cancellation pending jobs will still run, and the [stop] sequence will be triggered normally.
 *
 * In most cases manual cancellation should not be necessary, as structured concurrency will have the same effect if
 * the application's parent scope is cancelled.
 */
public interface EmbeddedApp {
  /** A unique identifier for this application. */
  public val id: EmbeddedAppId

  /** The current lifecycle state of this application. */
  public val state: StateFlow<EmbeddedAppState>

  /** Active configuration for this application. */
  public val config: EmbeddedAppConfiguration

  /**
   * Update the active [configuration][config] for the application. Configuration changes can only be applied if the
   * application is currently [idle][EmbeddedAppState.isIdle].
   *
   * @return Whether the active configuration was updated.
   */
  public fun update(newConfig: EmbeddedAppConfiguration): Boolean

  /**
   * Launch a suspending [Job] into the application's scope, returning its handle. The application must be
   * [running][EmbeddedAppState.RUNNING], otherwise an exception will be thrown.
   *
   * The specified [block] is guaranteed to never be cancelled unless [cancellable] is set to `true`, allowing request
   * dispatchers to finish when [cancel] is called.
   *
   * Note that even if [cancellable] is `true`, only [cancel] causes the application scope to be cancelled; calling
   * [stop] will not cancel any dispatched jobs.
   */
  public fun dispatch(cancellable: Boolean = false, block: suspend CoroutineScope.() -> Unit): Job

  /**
   * Start the application, accepting [dispatch] requests. If the application is already running or processing a
   * lifecycle transition, this method has no effect, otherwise it queues the startup transition. The returned [Job]
   * can be used to track the progress of the operation.
   *
   * The application [state] will transition to [EmbeddedAppState.STARTING] while performing all startup checks, and
   * then to [EmbeddedAppState.RUNNING] once finished.
   *
   * An application may be started more than once in its lifetime, provided it is [idle][EmbeddedAppState.isIdle].
   * Calling [stop] while the app is running will not invalidate its state or otherwise cause irreversible changes,
   * allowing future [start] calls to restart the application.
   */
  public fun start(): Job

  /**
   * Stop the application, rejecting all new [dispatch] requests, but allowing any pending jobs to complete. Once
   * fully stopped, the application may be restarted by calling [start], allowing the use of [dispatch] again.
   *
   * If the application is not running or is currently processing a state transition, this method has no effect,
   * otherwise it schedules a shutdown sequence and returns `true`. The returned [Job] can be used to track the
   * progress of the operation.
   *
   * The [state] will be set to [EmbeddedAppState.STOPPING] while waiting for pending jobs, and then to
   * [EmbeddedAppState.STOPPED] once shutdown is complete.
   *
   * To fully close the application and disallow any future restarts, see [cancel].
   */
  public fun stop(): Job

  /**
   * Cancel the application scope and invalidate it, allowing pending jobs to complete, but disabling future [start]
   * calls. This operation is **terminal** and should only be used to dispose the application when it is no longer
   * required.
   *
   * In most cases, it is preferrable to cancel an application's parent scope instead, allowing structured concurrency
   * to handle the cascading effect. In the event of parent cancellation, the application will still [stop] and allow
   * pending dispatch jobs to finish.
   *
   * If the application has already been disposed or is pending cancellation, this method returns `false`, otherwise
   * it schedules cancellation and returns `true`.
   */
  public fun cancel(): Boolean
}
