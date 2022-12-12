package elide.runtime.gvm.internals.intrinsics.js.stream

import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.js.JavaScriptStreams
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value

/** TBD. */
@Intrinsic internal class CoreStreamsIntrinsic : JavaScriptStreams, AbstractJsIntrinsic() {
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // not yet implemented
  }
}
