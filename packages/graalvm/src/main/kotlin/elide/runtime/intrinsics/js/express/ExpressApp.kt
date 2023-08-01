package elide.runtime.intrinsics.js.express

import org.graalvm.polyglot.Value
import elide.vm.annotations.Polyglot

/** An interface mapped to an Express app object, providing route configuration and other methods. */
public interface ExpressApp {
  /**
   * Register a GET route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun get(path: String, handler: Value)
  
  /**
   * Register a POST route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun post(path: String, handler: Value)

  /**
   * Register a PUT route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun put(path: String, handler: Value)

  /**
   * Register a DELETE route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun delete(path: String, handler: Value)

  /**
   * Register a HEAD route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun head(path: String, handler: Value)

  /**
   * Register an OPTIONS route [handler] at [path]. The [handler] must be a function and will receive two parameters
   * ([ExpressRequest] and [ExpressResponse], respectively).
   */
  @Polyglot public fun options(path: String, handler: Value)

  /** Bind the server to a given [port]. */
  @Polyglot public fun listen(port: Int) {
    listen(port, null)
  }

  /** Bind the server to a given [port], optionally invoking a [callback] when the socket is ready. */
  @Polyglot public fun listen(port: Int, callback: Value? = null)
}
