package elide.runtime.intrinsics.js.stream

import elide.vm.annotations.Polyglot

public interface ReadableStreamBYOBRequest {
  @get:Polyglot public val view: Any?
  @Polyglot public fun respond(bytesWritten: Long)
  @Polyglot public fun respondWithNewView(view: Any)
}
