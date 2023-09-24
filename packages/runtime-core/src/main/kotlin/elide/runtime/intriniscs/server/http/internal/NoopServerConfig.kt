package elide.runtime.intriniscs.server.http.internal

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intriniscs.server.http.HttpServerConfig

/** A stub implementation that doesn't register callbacks. */
@DelicateElideApi internal object NoopServerConfig : HttpServerConfig() {
  @Export override fun onBind(callback: PolyglotValue) {
    // nothing to do here
  }
}