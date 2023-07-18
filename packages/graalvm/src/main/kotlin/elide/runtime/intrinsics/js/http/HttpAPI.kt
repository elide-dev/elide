package elide.runtime.intrinsics.js.http

import elide.annotations.core.Polyglot
import org.graalvm.polyglot.Value

public interface HttpAPI {
  /**
   * Returns a new instance of http.Server.
   *
   * The [requestListener] is a function which is automatically added to the 'request' event.
   */
  @Polyglot public fun createServer(
    options: Any? = null,
    requestListener: Value? = null,
  ): Server
}