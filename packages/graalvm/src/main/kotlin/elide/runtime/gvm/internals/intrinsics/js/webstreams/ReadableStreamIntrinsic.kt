package elide.runtime.gvm.internals.intrinsics.js.webstreams

import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.ReadableStream

/** Implementation of readable streams (via the Web Streams standard). */
@Intrinsic(global = "ReadableStream") internal class ReadableStreamIntrinsic : AbstractJsIntrinsic() {
  /**
   * TBD.
   */
  internal class ReadableStreamImpl : ReadableStream {

  }

  /**
   * TBD.
   */
  internal companion object Factory : ReadableStream.Factory<ReadableStreamImpl> {
    /**
     * TBD.
     */
    override fun empty(): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(input: InputStream): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(reader: Reader): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(bytes: ByteArray): ReadableStreamImpl {
      TODO("Not yet implemented")
    }

    /**
     * TBD.
     */
    override fun wrap(buffer: ByteBuffer): ReadableStreamImpl {
      TODO("Not yet implemented")
    }
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // not yet implemented
  }
}
