package elide.runtime.intrinsics.js.stream

import elide.vm.annotations.Polyglot

public interface TransformStreamDefaultController {
  @get:Polyglot public val desiredSize: Double?

  @Polyglot public fun enqueue(chunk: Any? = null)
  @Polyglot public fun error(reason: Any? = null)
  @Polyglot public fun terminate()
}
