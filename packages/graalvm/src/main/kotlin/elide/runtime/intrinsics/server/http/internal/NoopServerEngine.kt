package elide.runtime.intrinsics.server.http.internal

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpRouter
import elide.runtime.intrinsics.server.http.HttpServerConfig
import elide.runtime.intrinsics.server.http.HttpServerEngine

/** A stub implementation that can be used to collect route handler references without starting a new server. */
@DelicateElideApi internal class NoopServerEngine(
  @Export override val config: HttpServerConfig,
  @Export override val router: HttpRouter,
) : HttpServerEngine {
  @Export override fun start() {
    // nothing to do here
  }
}