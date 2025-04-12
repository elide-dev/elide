package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.JsPromise
import elide.vm.annotations.Polyglot

public interface WritableStreamDefaultWriter {
  @get:Polyglot public val closed: JsPromise<Unit>
  @get:Polyglot public val ready: JsPromise<Unit>
  @get:Polyglot public val desiredSize: Double?

  @Polyglot public fun write(chunk: Any? = null): JsPromise<Unit>
  @Polyglot public fun releaseLock()
  @Polyglot public fun abort(reason: Any? = null): JsPromise<Unit>
  @Polyglot public fun close()
}
