package elide.embedded.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow.DROP_LATEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import elide.embedded.*
import elide.embedded.EmbeddedAppState.*
import elide.embedded.internal.EmbeddedAppImpl.Command.Start
import elide.embedded.internal.EmbeddedAppImpl.Command.Stop
import elide.embedded.internal.EmbeddedAppImpl.Companion.launch

/**
 * Implementation of the [EmbeddedApp] interface using an internal channel to dispatch lifecycle change requests to
 * ensure state transitions never run concurrently.
 *
 * Use the [launch] factory method to create a new instance and begin processing [start], [stop], and [update] calls.
 */
internal class EmbeddedAppImpl private constructor(
  override val id: EmbeddedAppId,
  config: EmbeddedAppConfiguration,
  context: CoroutineContext,
) : EmbeddedApp {
  /**
   * A request to update the current state of the application, for example, starting or stopping the app, or changing
   * the active configuration.
   */
  private sealed interface Command {
    /** Request to start the application if it is currently [idle][isIdle]. */
    @JvmInline value class Start(val handle: CompletableJob) : Command

    /** Request to stop the application if it is currently running. */
    @JvmInline value class Stop(val handle: CompletableJob) : Command
  }

  /**
   * Internal coroutine scope in which [dispatch] calls run, and where the [commandQueue] is processed.
   *
   * Waiting for dispatch calls to complete should be done using the [dispatchJob] instead of this scope's job, since
   * the command processing coroutine also runs here.
   */
  private val scope = CoroutineScope(context + SupervisorJob(context.job))

  /**
   * A [SupervisorJob] child of the application [scope], acting as a parent for all [dispatch] calls. This job allows
   * to wait for all active dispatch jobs to complete by using [Job.children].
   */
  private val dispatchJob = SupervisorJob(scope.coroutineContext.job)

  /**
   * A channel used as suspending queue for [commands][Command] that alter the application's state. The
   * [processCommandQueue] method consumes this queue and performs the necessary updates.
   *
   * This channel buffers at most one element, dropping offers if the buffer is full. It is recommended to call
   * [Channel.trySend] to prevent uncaught overflow issues.
   */
  private val commandQueue = Channel<Command>(capacity = Channel.BUFFERED, onBufferOverflow = DROP_LATEST)

  /**
   * Mutable thread-safe reference to the currently active configuration for this app. This value should only be
   * updated while the application is stopped to avoid inconsistent views during dispatch.
   */
  private val mutableConfig = atomic(config)

  /**
   * Mutable thread-safe state field, updated during [commandQueue] processing as the application transitions between
   * lifecycle states.
   */
  private val mutableState = MutableStateFlow(READY)

  override val config: EmbeddedAppConfiguration by mutableConfig

  override val state: StateFlow<EmbeddedAppState> get() = mutableState

  init {
    // consume lifecycle update commands
    scope.launch { processCommandQueue() }
  }

  /**
   * Consume the [commandQueue], calling the corresponding lifecycle method for each command while the app [scope] is
   * active. Once cancelled, [handleStop] will be called to ensure all pending dispatch jobs complete before the scope
   * is closed.
   */
  private suspend fun processCommandQueue() {
    commandQueue.consume {
      // read from the channel until the app scope is cancelled or the queue is closed
      withContext(NonCancellable) {
        while (true) when (val command = receiveCatching().getOrNull()) {
          is Start -> withHandle(command.handle) { handleStart() }
          is Stop -> withHandle(command.handle) { handleStop() }
          null -> break
        }
      }
    }

    // always stop the application, this allows pending dispatch jobs to finish
    // even in the event that the app scope is cancelled
    handleStop()
  }

  /**
   * Run the specified [block], completing the [handle] normally on success, or exceptionally if an exception is
   * thrown. Exceptions will be rethrown after causing the handle to fail.
   */
  private inline fun withHandle(handle: CompletableJob, block: () -> Unit) {
    try {
      block()
      handle.complete()
    } catch (cause: Throwable) {
      handle.completeExceptionally(cause)
      throw cause
    }
  }

  /**
   * Attempt to send the result of the [command] function through the [commandQueue], attaching a [Job] to it. The
   * returned [Job] will be already complete if the command cannot be queued, to avoid consumer starvation.
   */
  private inline fun trySubmit(command: (CompletableJob) -> Command): Job {
    val handle = Job()

    // in case of failure, complete the job to avoid starving the caller
    if (commandQueue.trySend(command(handle)).isFailure) handle.complete()
    return handle
  }

  /**
   * Handle an application start request, running the startup sequence and updating the [state] to [RUNNING] on
   * success.
   *
   * If the application is not currently [idle][isIdle], this method has no effect and returns `false`, otherwise it
   * returns `true`.
   */
  private suspend fun handleStart(): Boolean {
    // cannot start if not idle, fail silently
    if (!state.value.isIdle) return false

    mutableState.value = STARTING
    // TODO(@darvld) startup sequence (nothing required yet)
    // TODO(@darvld) support lifecycle pipeline hooks

    // suspending placeholder for future operations
    yield()

    mutableState.value = RUNNING
    return true
  }

  /**
   * Handle an application stop request, setting the [state] to [STOPPING] until all pending [dispatch] jobs complete,
   * then updating to [STOPPED] once the shutdown sequence is finished.
   *
   * This method is non-cancellable, so it can be used to safely stop the application in a coroutine scope in the
   * event of parent cancellation.
   *
   * If the application is currently [idle][isIdle], this method has no effect and returns `false`, otherwise it
   * returns `true`.
   */
  private suspend fun handleStop(): Boolean {
    // the shutdown sequence must not be cancelled, to ensure all pending
    // requests are properly dispatched and hooks are invoked
    return withContext(NonCancellable) {
      if (state.value.isIdle) return@withContext false

      mutableState.value = STOPPING
      // wait for pending dispatch jobs, here we join sequentially since it doesn't
      // affect the total waiting time (the slowest child sets the duration)
      dispatchJob.children.forEach { it.join() }

      // TODO(@darvld) shutdown sequence (nothing required yet)
      // TODO(@darvld) support lifecycle pipeline hooks

      mutableState.value = STOPPED
      return@withContext true
    }
  }

  @OptIn(DelicateCoroutinesApi::class) override fun cancel(): Boolean {
    // already closed, no running jobs and no commands can be sent
    if (!scope.isActive && commandQueue.isClosedForSend) return false

    // forcibly shut down the queue and all pending operations
    // cancelling the scope will also stop the application
    commandQueue.close()
    scope.cancel()

    return true
  }

  override fun dispatch(cancellable: Boolean, block: suspend CoroutineScope.() -> Unit): Job {
    check(state.value == RUNNING) { "Illegal dispatch while not running (app is $state)" }

    // launch in the app scope, but override the parent job with the dispatch root;
    // this allows lifecycle methods to wait only for children of the dispatch job
    return scope.launch(dispatchJob) {
      // dispatched jobs must not be cancelled, to allow pending requests to correctly
      // execute if the app/runtime stops, while complying with structured concurrency
      withContext(NonCancellable) { block() }
    }
  }

  override fun start(): Job {
    return trySubmit(::Start)
  }

  override fun stop(): Job {
    return trySubmit(::Stop)
  }

  override fun update(newConfig: EmbeddedAppConfiguration): Boolean {
    if (newConfig == config || !state.value.isIdle) return false
    mutableConfig.value = newConfig

    return true
  }

  internal companion object {
    /**
     * Launch a new [EmbeddedApp] with the specified [id] and initial [config], scoped to a [context]. The application
     * will begin processing state updates immediately.
     *
     * Cancelling the [context] after launch will correctly [stop] the application, allowing all pending jobs to
     * complete.
     */
    fun launch(
      id: EmbeddedAppId,
      config: EmbeddedAppConfiguration,
      context: CoroutineContext
    ): EmbeddedAppImpl {
      return EmbeddedAppImpl(id, config, context)
    }
  }
}
