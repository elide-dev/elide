package elide.embedded

import kotlinx.coroutines.Deferred
import elide.embedded.http.EmbeddedResponse

/**
 * A dispatcher for incoming [EmbeddedCall]s, which schedules execution in the scope of a guest application using the
 * active runtime configuration to manage the guest evaluation context.
 */
public fun interface EmbeddedCallDispatcher {
  /**
   * Dispatch an incoming [call] through a guest [app], returning a deferred value which tracks the execution
   * progress.
   */
  public fun dispatch(call: EmbeddedCall, app: EmbeddedApp): Deferred<EmbeddedResponse>
}