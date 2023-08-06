/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.intrinsics.js.express

import io.micronaut.core.async.publisher.Publishers
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollMode.EDGE_TRIGGERED
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpMethod
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import reactor.netty.http.HttpProtocol
import reactor.netty.http.HttpProtocol.*
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import kotlin.reflect.KClass
import elide.runtime.Logging
import elide.runtime.intrinsics.js.express.ExpressApp
import elide.vm.annotations.Polyglot

/**
 * A function that tests whether an incoming [HttpServerRequest] should be passed to a handler, using a template string
 * and optionally filtering by HTTP method. Path variables included in the template will be captured by the matcher and
 * added to the request proxy.
 */
internal typealias ExpressPathMatcher = (path: String, method: String, proxy: ProxyObject) -> Boolean

/**
 * An [ExpressApp] implemented as a wrapper around a Reactor Netty server.
 *
 * A server builder is used internally and can be configured by methods like [get]. Calling [listen] will cause the
 * server to be built and bound to the specified port.
 */
internal class ExpressAppIntrinsic(private val context: ExpressContext) : ExpressApp {
  /** Represents a route handler registered by guest code. */
  private data class Handler(
    val matches: ExpressPathMatcher,
    val handle: Value,
  )

  private val logging by lazy { Logging.of(ExpressAppIntrinsic::class) }

  /** Collection of ordered handlers registered by guest code, used to process incoming requests. */
  private val pipeline = mutableListOf<Handler>()

  @Polyglot override fun get(path: String, handler: Value) = registerHandler(handler, path, HttpMethod.GET)
  @Polyglot override fun post(path: String, handler: Value) = registerHandler(handler, path, HttpMethod.POST)
  @Polyglot override fun put(path: String, handler: Value) = registerHandler(handler, path, HttpMethod.PUT)
  @Polyglot override fun delete(path: String, handler: Value) = registerHandler(handler, path, HttpMethod.DELETE)
  @Polyglot override fun head(path: String, handler: Value) = registerHandler(handler, path, HttpMethod.HEAD)
  @Polyglot override fun options(path: String, handler: Value) = registerHandler(handler, path, HttpMethod.OPTIONS)
  @Polyglot override fun use(handler: Value) = registerHandler(handler)
  @Polyglot override fun use(path: String, handler: Value) = registerHandler(handler, path)

  private enum class IoMultiplexer {
    NIO, EPOLL, KQUEUE, IO_URING
  }

  private class TransportImpl<T>(
    val eventLoopGroup: EventLoopGroup,
    val socketChannel: KClass<T>,
    val multiplexer: IoMultiplexer,
    val channelOptions: HttpServer.() -> Unit = { },
  ) where T: ServerSocketChannel

  @Suppress("UNUSED")
  private fun resolveEventLoop(): TransportImpl<*> = when {
    // prefer `io_uring` if available (Linux-only, modern kernels)
    io.netty.incubator.channel.uring.IOUring.isAvailable() -> TransportImpl(
      io.netty.incubator.channel.uring.IOUringEventLoopGroup(Runtime.getRuntime().availableProcessors()),
      io.netty.incubator.channel.uring.IOUringServerSocketChannel::class,
      IoMultiplexer.IO_URING,
    ) {
      childOption(io.netty.incubator.channel.uring.IOUringChannelOption.SO_REUSEADDR, true)
      childOption(io.netty.incubator.channel.uring.IOUringChannelOption.TCP_NODELAY, true)
      childOption(io.netty.incubator.channel.uring.IOUringChannelOption.TCP_DEFER_ACCEPT, 256)
      childOption(io.netty.incubator.channel.uring.IOUringChannelOption.TCP_QUICKACK, true)
    }

    // next up, prefer `epoll` if available (Linux-only, nearly all kernels)
    io.netty.channel.epoll.Epoll.isAvailable() -> TransportImpl(
      io.netty.channel.epoll.EpollEventLoopGroup(),
      io.netty.channel.epoll.EpollServerSocketChannel::class,
      IoMultiplexer.EPOLL,
    ) {
      childOption(io.netty.channel.epoll.EpollChannelOption.SO_REUSEADDR, true)
      childOption(io.netty.channel.epoll.EpollChannelOption.TCP_NODELAY, true)
      childOption(io.netty.channel.epoll.EpollChannelOption.TCP_DEFER_ACCEPT, 256)
      childOption(io.netty.channel.epoll.EpollChannelOption.TCP_QUICKACK, true)
      childOption(io.netty.channel.epoll.EpollChannelOption.EPOLL_MODE, EDGE_TRIGGERED)
    }

    // next up, opt for `kqueue` on Unix-like systems
    io.netty.channel.kqueue.KQueue.isAvailable() -> TransportImpl(
      io.netty.channel.kqueue.KQueueEventLoopGroup(),
      io.netty.channel.kqueue.KQueueServerSocketChannel::class,
      IoMultiplexer.KQUEUE,
    ) {
      childOption(io.netty.channel.kqueue.KQueueChannelOption.SO_REUSEADDR, true)
      childOption(io.netty.channel.kqueue.KQueueChannelOption.TCP_NODELAY, true)
      childOption(io.netty.channel.kqueue.KQueueChannelOption.TCP_NOPUSH, true)
      childOption(io.netty.channel.kqueue.KQueueChannelOption.TCP_FASTOPEN_CONNECT, true)
    }

    // otherwise, fallback to NIO
    else -> TransportImpl(
      NioEventLoopGroup(),
      NioServerSocketChannel::class,
      IoMultiplexer.NIO,
    )
  }

  @Polyglot override fun listen(port: Int, callback: Value?): Unit = resolveEventLoop().let { transport ->
    // configure all the route handlers, set the port and bind the socket
    HttpServer.create().apply {
      Logging.named("gvm:js.console").info("Using transport ${transport.multiplexer.name}")

      // configure event loop group
      runOn { native ->
        if (!native) {
          error("Failed to load native transport")
        } else {
          transport.eventLoopGroup
        }
      }

      // set `SO_REUSEADDR` to allow multi-binding
      childOption(ChannelOption.SO_REUSEADDR, true)
      childOption(ChannelOption.TCP_NODELAY, true)
      childOption(ChannelOption.TCP_FASTOPEN_CONNECT, true)

      // maximum keep-alive
      maxKeepAliveRequests(10_000_000)
      protocol(HTTP11, H2C)
      noSSL()

      // warmup the server (load native libs, start event loops, etc.)
      warmup().block()

      handle { req, res ->
        // TODO(@darvld): restore use of the pipeline when supported by the disruptor context manager
        // val responseWrapper = ExpressResponseIntrinsic(res)
        // val requestProxy = ExpressRequestIntrinsic.from(req)

        if (req.uri() == "/health") {
          res.send(Publishers.just(Unpooled.wrappedBuffer(okResponse)))
        } else context.useGuest {
          // handlePipelineStage(req, responseWrapper, requestProxy)
          res.send(Publishers.just(Unpooled.wrappedBuffer(helloResponse)))
        }

        // responseWrapper.end()
      }.port(port).bindNow()

      // prevent the JVM from exiting while the server is running
      context.pin()

      // notify listeners
      callback?.executeVoid()
    }
  }

  private fun registerHandler(handle: Value, path: String? = null, method: HttpMethod? = null) {
    pipeline.add(
      Handler(
        matches = requestMatcher(path, method?.name()),
        handle,
      ),
    )
  }

  private fun handlePipelineStage(
    incomingRequest: HttpServerRequest,
    responseWrapper: ExpressResponseIntrinsic,
    requestProxy: ProxyObject,
    stage: Int = 0,
  ) {
    logging.debug { "Handling pipeline stage: $stage" }

    // get the next handler in the pipeline (or end if no more handlers remaining)
    val handler = pipeline.getOrNull(stage) ?: return

    if (handler.matches(incomingRequest.fullPath(), incomingRequest.method().name(), requestProxy)) {
      logging.debug { "Handler condition matches request at stage $stage" }
      // process this stage, giving the option to continue to the next stage
      handler.handle.executeVoid(
        requestProxy,
        responseWrapper,
        // "next" optional argument for express handlers
        ProxyExecutable {
          logging.debug { "Executable proxy for 'next' handler called from stage $stage" }
          handlePipelineStage(incomingRequest, responseWrapper, requestProxy, stage + 1)
        },
      )
    } else {
      logging.debug { "Handler condition does not match request at stage $stage, bypassing" }
      // skip this stage
      handlePipelineStage(incomingRequest, responseWrapper, requestProxy, stage + 1)
    }
  }

  companion object {
    private val okResponse = "OK".encodeToByteArray()
    private val helloResponse = "Hello".encodeToByteArray()

    private const val MATCHER_NAME_GROUP = "name"

    /** Regex matching path variable templates specified by a guest handler */
    private val PathVariableRegex = Regex(":(?<$MATCHER_NAME_GROUP>\\w+)")

    /**
     * Returns a function that tests whether an incoming [HttpServerRequest] should be passed to a handler, using a
     * [template] string and optionally filtering by HTTP [method]. Path variables included in the [template] will be
     * captured by the matcher and added to the request proxy.
     *
     * @param template An optional template string used to match incoming request paths, can contain variable matchers
     * in the format specified by the Express.js documentation. If not provided, all requests are matched regardless of
     * the path, unless the [method] option is set.
     * @param method An optional HTTP method filter. If not specified, requests are only filtered by path, as specified
     * by the [template].
     *
     * @return A matcher function receiving two arguments: the [HttpServerRequest] to be tested, and a [Value] proxy
     * to store the extracted variables.
     */
    fun requestMatcher(
      template: String? = null,
      method: String? = null
    ): ExpressPathMatcher {
      // keep a record of all path variables in the template
      val variables = mutableListOf<String>()

      // create a matching pattern using the provided path template
      val pattern = template?.replace(PathVariableRegex) { result ->
        // replace express path variable matchers with named capture groups
        val paramName = result.groups[MATCHER_NAME_GROUP]?.value ?: error("Invalid path matcher")
        variables.add(paramName)

        "(?<$paramName>[^\\/]+)"
      }?.let(::Regex)

      return matcher@{ incomingPath, incomingMethod, proxy ->
        // Filter by HTTP method
        if(method != null && method != incomingMethod) return@matcher false

        // if no matcher template is specified, accept all paths
        if(pattern == null) return@matcher true

        // otherwise return true when the pattern matches the requested path
        pattern.matchEntire(incomingPath)?.also { match ->
          val requestParams = proxy.getMember("params") as ProxyObject

          // extract path variables and add them to the request
          for(variable in variables) match.groups[variable]?.let {
            requestParams.putMember(variable, Value.asValue(it.value))
          }
        } != null
      }
    }
  }
}
