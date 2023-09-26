package elide.runtime.intrinsics.server.http.netty

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpServerConfig

/** Configuration options specific to the [NettyServerEngine]. */
@DelicateElideApi internal class NettyServerConfig : HttpServerConfig() {
  /**
   * The Netty transport to be used by the server, defaults to "auto", which detects the preferred transport for the
   * current platform.
   *
   * Explicitly setting this value will attempt to force loading the specified transport, failing if it's not
   * available.
   */
  @Export var transport: String? = TRANSPORT_DETECT

  /** Resolve a [NettyTransport], honoring the [transport] option. */
  internal fun resolveTransport(): NettyTransport<*> = when (transport) {
    TRANSPORT_IO_URING -> IOUringTransport
    TRANSPORT_EPOLL -> EpollTransport
    TRANSPORT_KQUEUE -> KQueueTransport
    TRANSPORT_NIO -> NioTransport

    // resolve by default
    else -> NettyTransport.resolve()
  }

  companion object {
    /** Use with [transport] to automatically detect the best transport for the current platform. */
    const val TRANSPORT_DETECT = "auto"

    /** Use with [transport] to force using the [IOUringTransport] or fail if it's not available. */
    const val TRANSPORT_IO_URING = "io_uring"

    /** Use with [transport] to force using the [EpollTransport] or fail if it's not available. */
    const val TRANSPORT_EPOLL = "epoll"

    /** Use with [transport] to force using the [KQueueTransport] or fail if it's not available. */
    const val TRANSPORT_KQUEUE = "kqueue"

    /** Use with [transport] to force using the [NioTransport] or fail if it's not available. */
    const val TRANSPORT_NIO = "nio"
  }
}