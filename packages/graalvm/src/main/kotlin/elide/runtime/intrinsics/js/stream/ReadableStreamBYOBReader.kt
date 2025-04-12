package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.vm.annotations.Polyglot

public interface ReadableStreamBYOBReader : ReadableStreamReader {
  @Polyglot public fun read(view: Any, options: Any? = null): JsPromise<ReadResult>
}
