package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.AbortSignal
import elide.vm.annotations.Polyglot

public interface WritableStreamDefaultController {
  @get:Polyglot public val signal: AbortSignal
  @Polyglot public fun error(e: Any? = null)
}
