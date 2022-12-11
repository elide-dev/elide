package elide.runtime.gvm.intrinsics.js.webstreams

import elide.runtime.gvm.intrinsics.Intrinsic
import elide.runtime.gvm.intrinsics.js.AbstractJsIntrinsic

/** Implementation of readable streams (via the Web Streams standard). */
@Intrinsic(global = "ReadableStream") internal class ReadableStreamIntrinsic : ReadableStream, AbstractJsIntrinsic() {
  // Nothing yet.
}
