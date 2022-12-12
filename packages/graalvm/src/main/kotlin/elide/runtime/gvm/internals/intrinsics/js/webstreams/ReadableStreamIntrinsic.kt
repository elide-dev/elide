package elide.runtime.gvm.internals.intrinsics.js.webstreams

import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.js.ReadableStream
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value

/** Implementation of readable streams (via the Web Streams standard). */
@Intrinsic(global = "ReadableStream") internal class ReadableStreamIntrinsic : ReadableStream, AbstractJsIntrinsic() {
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // not yet implemented
  }
}
