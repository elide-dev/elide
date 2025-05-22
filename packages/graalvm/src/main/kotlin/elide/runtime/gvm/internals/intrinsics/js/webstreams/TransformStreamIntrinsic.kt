package elide.runtime.gvm.internals.intrinsics.js.webstreams

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.GuestIntrinsic

/** Implementation of transform streams (via the Web Streams standard). */
@Intrinsic(global = "TransformStream") internal class TransformStreamIntrinsic : AbstractJsIntrinsic() {
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // not yet implemented
  }
}
