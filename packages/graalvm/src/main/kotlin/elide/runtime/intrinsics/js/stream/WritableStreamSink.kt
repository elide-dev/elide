package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.JsPromise

public interface WritableStreamSink {
  public val type: Any

  public fun start(controller: WritableStreamDefaultController)
  public fun write(chunk: Any? = null, controller: WritableStreamDefaultController): JsPromise<Unit>
  public fun close(): JsPromise<Unit>
  public fun abort(reason: Any? = null): JsPromise<Unit>
}
