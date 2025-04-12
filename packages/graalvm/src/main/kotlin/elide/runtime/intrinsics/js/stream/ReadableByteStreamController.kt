package elide.runtime.intrinsics.js.stream

import elide.vm.annotations.Polyglot

public interface ReadableByteStreamController : ReadableStreamController {
  @get:Polyglot public val byobRequest: ReadableStreamBYOBRequest

  @Polyglot public fun close()
  @Polyglot public fun enqueue(chunk: Any? = null)
  @Polyglot public fun error(reason: Any? = null)
}
