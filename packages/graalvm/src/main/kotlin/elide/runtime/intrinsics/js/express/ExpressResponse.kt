package elide.runtime.intrinsics.js.express

import org.graalvm.polyglot.Value
import elide.vm.annotations.Polyglot

/**
 * Represents a very basic binding for a response object passed to an Express handler function.
 * 
 * Note that this interface does not cover many fields and methods expected by regular Express applications, as it is
 * intended only for demonstration purposes.
 */
public interface ExpressResponse {
  /** Sends this response with the given [body]. */
  @Polyglot public fun send(body: Value)
}
