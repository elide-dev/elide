/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.intrinsics.server.http.v2

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.core.RuntimeLatch
import elide.runtime.intrinsics.server.http.netty.NettyTransport
import elide.runtime.intrinsics.server.http.v2.channels.NettyHttpContextAdapter

/**
 * Base class for guest intrinsics that allow binding HTTP servers. Use [bind] to start listening for requests.
 *
 * Implementations are expected to add guest-accessible members to control the server's lifecycle and call the methods
 * in this base class from there.
 */
public abstract class HttpServer : AutoCloseable {
  private val serverRunning = AtomicBoolean(false)
  private var eventLoopGroup: EventLoopGroup? = null
  private var serverChannel: Channel? = null

  protected abstract val runtimeLatch: RuntimeLatch

  /** Whether the server is currently running and accepting requests. */
  protected val isRunning: Boolean get() = serverRunning.get()

  /**
   * Create a new handler instance to be added to a Netty channel. Handlers are only added to a single channel at a
   * time, so they can be stateful if needed.
   */
  protected abstract fun acquireHandler(): HttpContextHandler

  /**
   * Provide a context factory to be used for creating contexts on incoming requests. The same factory instance can be
   * returned multiple times as long as it is thread-safe and reusable between channels.
   */
  protected abstract fun acquireFactory(): HttpContextFactory<*>

  /**
   * Bind to the given [port] and begin accepting requests. Calling this method multiple times or from a non-owner
   * thread has no effect.
   */
  public fun bind(port: Int): Boolean = bind(port, NettyTransport.resolve())

  /**
   * Bind to the given [port] and begin accepting requests using the specified [transport]. Calling this method
   * multiple times or from a non-owner thread has no effect.
   */
  internal fun bind(port: Int, transport: NettyTransport<*>): Boolean {
    logging.debug("Starting server")

    // allow this call only once
    if (!serverRunning.compareAndSet(false, true)) {
      logging.debug("Server already running, ignoring bind call")
      return false
    }

    // acquire platform-specific Netty components
    logging.debug { "Using transport: $transport" }

    with(ServerBootstrap()) {
      // server channel options
      option(ChannelOption.SO_BACKLOG, DEFAULT_SERVER_BACKLOG)
      option(ChannelOption.SO_REUSEADDR, true)

      // apply transport options
      logging.debug { "Applying options from $transport" }
      transport.bootstrap(this)

      eventLoopGroup = config().group()

      // attach custom handler pipeline and configure client channels
      childHandler(channelInitializer())
      childOption(ChannelOption.SO_REUSEADDR, true)
      childOption(ChannelOption.TCP_NODELAY, true)

      // start listening
      val address = InetSocketAddress(port)
      serverChannel = bind(address).sync().channel()

      logging.info { "Server listening at $address" }
      runtimeLatch.retain()
    }

    return true
  }

  override fun close() {
    if (!serverRunning.compareAndSet(true, false)) return
    logging.debug { "Closing server" }

    runCatching { serverChannel?.close()?.sync() }
    serverChannel = null

    runCatching { eventLoopGroup?.shutdownGracefully()?.sync() }
    eventLoopGroup = null

    runtimeLatch.release()
  }

  private fun channelInitializer() = object : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
      logging.debug { "Initializing channel: $ch" }

      val contextHandler = NettyHttpContextAdapter(
        contextFactory = acquireFactory(),
        contextHandler = acquireHandler(),
      )

      ch.pipeline().addLast(HttpServerCodec(), contextHandler)
    }
  }

  public companion object {
    private val logging by lazy { Logging.of(HttpServer::class) }

    /** Backlog size for the server socket. */
    private const val DEFAULT_SERVER_BACKLOG = 8192
  }
}
