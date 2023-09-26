package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/** Represents an HTTP response returned by the server, accessible from guest code. */
@DelicateElideApi public interface HttpResponse {
  /**
   * Exported method allowing guest code to send a response to the client with the given [status] code and [body].
   */
  @Export public fun send(status: Int, body: PolyglotValue?)
}