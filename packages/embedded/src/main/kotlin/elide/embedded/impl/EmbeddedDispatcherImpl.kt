package elide.embedded.impl

import elide.embedded.api.EmbeddedRuntime.EmbeddedDispatcher
import elide.embedded.api.UnaryNativeCall

internal class EmbeddedDispatcherImpl  : EmbeddedDispatcher {
  override suspend fun handle(call: UnaryNativeCall) {
    // TODO(@darvld): implement call dispatch
  }
}
