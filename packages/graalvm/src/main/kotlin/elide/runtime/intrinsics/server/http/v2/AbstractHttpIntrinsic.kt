/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.intrinsics.server.http.netty.NettyTransport
import elide.runtime.intrinsics.server.http.v2.channels.NettyHttpContextAdapter

/**
 * Base class for guest intrinsics that allow binding HTTP servers. Use [bind] to start listening for requests.
 *
 * Implementations are expected to add guest-accessible members to control the server's lifecycle and call the methods
 * in this base class from there.
 */
public abstract class AbstractHttpIntrinsic {
  private val serverRunning = AtomicBoolean(false)
  private val ownerThread = ThreadLocal<Boolean>()

  /** Whether the server is currently running and accepting requests. */
  protected val isRunning: Boolean get() = serverRunning.get()

  /**
   * Whether the current thread is the owner of the intrinsic itself, that is, the thread on which the server was
   * originally created. This sets it apart from "mirror" threads that are spun up during request handling.
   *
   * Operations that affect the lifecycle of the server must only run on the owner thread.
   */
  protected val isOwnerThread: Boolean get() = ownerThread.get() ?: false

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
   *
   * Successfully calling this method will result in the calling thread being labeled as the [owner][isOwnerThread]
   * thread.
   */
  internal fun bind(port: Int, transport: NettyTransport<*> = NettyTransport.resolve()) {
    logging.debug("Starting server")

    // allow this call only once
    if (!serverRunning.compareAndSet(false, true)) {
      logging.debug("Server already running, ignoring bind call")
      return
    }

    if (!ownerThread.get()) {
      logging.debug("Bind called from a mirror thread, ignoring")
      return
    }

    ownerThread.set(true)

    // acquire platform-specific Netty components
    logging.debug { "Using transport: $transport" }

    with(ServerBootstrap()) {
      // server channel options
      option(ChannelOption.SO_BACKLOG, DEFAULT_SERVER_BACKLOG)
      option(ChannelOption.SO_REUSEADDR, true)

      // apply transport options
      logging.debug { "Applying options from $transport" }
      transport.bootstrap(this)

      // attach custom handler pipeline and configure client channels
      childHandler(channelInitializer())
      childOption(ChannelOption.SO_REUSEADDR, true)
      childOption(ChannelOption.TCP_NODELAY, true)

      // start listening
      val address = InetSocketAddress(port)
      bind(address).sync().channel()

      logging.debug { "Server listening at $address" }
    }
  }

  private fun channelInitializer() = object : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
      val contextHandler = NettyHttpContextAdapter(
        contextFactory = acquireFactory(),
        contextHandler = acquireHandler(),
      )

      ch.pipeline().addLast(HttpServerCodec(), contextHandler)
    }
  }

  public companion object {
    private val logging by lazy { Logging.of(AbstractHttpIntrinsic::class) }

    /** Backlog size for the server socket. */
    private const val DEFAULT_SERVER_BACKLOG = 8192
  }
}
