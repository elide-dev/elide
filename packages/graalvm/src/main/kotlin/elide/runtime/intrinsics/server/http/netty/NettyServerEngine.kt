package elide.runtime.intriniscs.server.http.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import org.graalvm.polyglot.HostAccess.Export
import java.net.InetSocketAddress
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.intriniscs.server.http.HttpServerEngine
import elide.runtime.intriniscs.server.http.internal.PipelineRouter

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
  /** Private logger instance. */
  private val logging by lazy { Logging.of(HttpServerEngine::class) }

  /** Construct a new [ChannelHandler] used as initializer for client channels. */
  private fun prepareChannelInitializer(): ChannelHandler {
    return NettyChannelInitializer(NettyRequestHandler(router))
  }

  /**
   * Start listening at the port specified by [config], using a native transport resolved for the current platform.
   */
  @Export override fun start() {
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