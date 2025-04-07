package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableByteStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamBYOBRequest
import elide.vm.annotations.Polyglot

internal class ReadableStreamByteControllerToken(
  private val stream: ReadableByteStream
) : ReadableByteStreamController {
  @get:Polyglot override val desiredSize: Double? get() = stream.desiredSize()
  @get:Polyglot override val byobRequest: ReadableStreamBYOBRequest? get() = stream.getRequest()

  @Polyglot override fun enqueue(chunk: Value?) {
    if (chunk == null) throw TypeError.create("Cannot enqueue null or undefined chunk")
    stream.fulfillOrEnqueue(chunk)
  }

  @Polyglot override fun error(reason: Any?) = stream.error(reason)
  @Polyglot override fun close() = stream.close()
}
