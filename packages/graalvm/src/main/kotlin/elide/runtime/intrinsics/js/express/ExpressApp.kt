package elide.runtime.intrinsics.js.express

import elide.annotations.core.Polyglot
import org.graalvm.polyglot.Value

/** An interface mapped to an Express app object, providing route configuration and other methods. */
public interface ExpressApp {
  /**
   * Register a GET route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun get(path: String, handler: Value)
  
  /** Bind the server to a given [port], optionally invoking a [callback] when the socket is ready. */
  @Polyglot public fun listen(port: Int, callback: Value? = null)
}