package elide.runtime.gvm.internals.intrinsics.js.stream

import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.js.JavaScriptStreams

/** TBD. */
@Intrinsic internal class CoreStreamsIntrinsic : JavaScriptStreams, AbstractJsIntrinsic() {
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // not yet implemented
  }
}
