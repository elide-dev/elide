package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi

/** Represents an incoming HTTP request received by the server, accessible by guest code. */
@DelicateElideApi public interface HttpRequest {
  /** The URI (path) for this request. */
  @get:Export public val uri: String

  /** The HTTP method for this request */
  @get:Export public val method: HttpMethod
}