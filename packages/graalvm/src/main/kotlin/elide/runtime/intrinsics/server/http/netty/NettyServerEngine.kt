package elide.runtime.intrinsics.server.http.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import org.graalvm.polyglot.HostAccess.Export
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpServerEngine
import elide.runtime.intrinsics.server.http.internal.PipelineRouter

/**
 * Netty-based [HttpServerEngine] implementation.
 *
 * This class resolves a platform-specific [NettyTransport], which uses native libraries if available for increased
 * performance.
 */
@DelicateElideApi internal class NettyServerEngine(
  @Export override val config: NettyServerConfig,
  @Export override val router: PipelineRouter,
) : HttpServerEngine {
  /** Thread-safe flag to signal  */
  private val serverRunning = AtomicBoolean(false)

  /** Private logger instance. */
  private val logging by lazy { Logging.of(NettyServerEngine::class) }

  @get:Export override val running: Boolean get() = serverRunning.get()

  /** Construct a new [ChannelHandler] used as initializer for client channels. */
  private fun prepareChannelInitializer(): ChannelHandler {
    return NettyChannelInitializer(NettyRequestHandler(router))
  }

  @Export override fun start() {
    logging.debug("Starting server")

    // allow this call only once
    if (!serverRunning.compareAndSet(false, true)) {
      logging.debug("Server already running, ignoring start() call")
      return
    }

    // acquire platform-specific Netty components
    val transport = config.resolveTransport()
    logging.debug { "Using transport: $transport" }

    with(ServerBootstrap()) {
      // server channel options
      option(ChannelOption.SO_BACKLOG, SERVER_BACKLOG)
      option(ChannelOption.SO_REUSEADDR, true)

      // apply transport options
      logging.debug { "Applying options from $transport" }
      transport.bootstrap(this)

      // attach custom handler pipeline and configure client channels
      childHandler(prepareChannelInitializer())
      childOption(ChannelOption.SO_REUSEADDR, true)

      // start listening
      val address = InetSocketAddress(config.port)
      bind(address).sync().channel()

      // notify listeners if applicable
      logging.debug { "Server listening at $address" }
      config.onBindCallback?.invoke()
    }
  }

  private companion object {
    /** Backlog size for the server socket. */
    private const val SERVER_BACKLOG = 8192
  }
}