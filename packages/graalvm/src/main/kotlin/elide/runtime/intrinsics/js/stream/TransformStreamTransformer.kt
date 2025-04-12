package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream

public interface TransformStreamTransformer {
  public val readableType: ReadableStream.Type
  public val writableType: Any

  public fun start(controller: TransformStreamDefaultController)
  public fun flush(controller: TransformStreamDefaultController): JsPromise<Unit>
  public fun transform(chunk: Any? = null, controller: TransformStreamDefaultController): JsPromise<Unit>
  public fun cancel(reason: Any? = null): JsPromise<Unit>
}
