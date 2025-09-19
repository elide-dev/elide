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
package elide.runtime.intrinsics.server.http.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.intrinsics.server.http.HttpServerEngine
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import elide.vm.annotations.Polyglot

// Properties accessible to guest code on the server engine.
private val HTTP_SERVER_INTRINSIC_PROPS_AND_METHODS = arrayOf(
  "config",
  "router",
  "running",
  "start",
)

/**
 * Netty-based [HttpServerEngine] implementation.
 *
 * This class resolves a platform-specific [NettyTransport], which uses native libraries if available for increased
 * performance.
 */
@DelicateElideApi internal class NettyServerEngine(
  @get:Polyglot override val config: NettyServerConfig,
  @get:Polyglot override val router: PipelineRouter,
  private val exec: GuestExecutorProvider,
) : HttpServerEngine, ProxyObject {
  /** Thread-safe flag to signal  */
  private val serverRunning = AtomicBoolean(false)

  /** Private logger instance. */
  private val logging by lazy { Logging.of(NettyServerEngine::class) }

  @get:Polyglot override val running: Boolean get() = serverRunning.get()

  /** Construct a new [ChannelHandler] used as initializer for client channels. */
  private fun prepareChannelInitializer(): ChannelHandler = NettyChannelInitializer(NettyRequestHandler(
    router,
    exec,
    scheme = "http:",
    host = config.host,
    port = config.port.toUShort(),
  ))

  @Polyglot override fun start() {
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
      childOption(ChannelOption.TCP_NODELAY, true)

      // start listening
      val address = InetSocketAddress(config.port)
      bind(address).sync().channel()

      // notify listeners if applicable
      logging.debug { "Server listening at $address" }
      kotlin.runCatching { System.err.println("[elide-netty] BOUND " + address) }

      config.onBindCallback?.invoke()
    }
  }

  override fun hasMember(key: String?): Boolean = key != null && key in HTTP_SERVER_INTRINSIC_PROPS_AND_METHODS
  override fun getMemberKeys(): Array<String> = HTTP_SERVER_INTRINSIC_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) {
    // no-op
  }

  override fun removeMember(key: String?): Boolean {
    return false  // not allowed
  }

  override fun getMember(key: String?): Any? = when (key) {
    "config" -> config
    "router" -> router
    "running" -> running
    "start" -> ProxyExecutable { start() }
    null -> null
    else -> null
  }

  private companion object {
    /** Backlog size for the server socket. */
    private const val SERVER_BACKLOG = 8192
  }
}
