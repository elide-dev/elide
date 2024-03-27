package elide.embedded

import kotlinx.coroutines.Job

/**
 * A dispatcher for incoming [EmbeddedCall]s, which schedules execution in the scope of a guest application using the
 * active runtime configuration to manage the guest evaluation context.
 */
public fun interface EmbeddedCallDispatcher {
  /**
   * Dispatch an incoming [call] through a guest [app], returning an observable [Job] which tracks the execution
   * progress. Alternatively, using [EmbeddedCall.await] will provide the result of the operation.
   */
  public fun dispatch(call: EmbeddedCall, app: EmbeddedApp): Job
}