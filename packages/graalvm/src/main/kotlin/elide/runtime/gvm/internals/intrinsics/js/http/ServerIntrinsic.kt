package elide.runtime.gvm.internals.intrinsics.js.http

import elide.runtime.intrinsics.js.http.IncomingMessage
import elide.runtime.intrinsics.js.http.OutgoingMessage
import elide.runtime.intrinsics.js.http.Server
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*

internal class ServerIntrinsic(
  private val requestListener: (IncomingMessage, OutgoingMessage) -> Unit,
) : Server {
  inner class HttpChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(channel: SocketChannel) {
      channel.pipeline()
        .addLast(HttpServerCodec())
        .addLast(HttpObjectAggregator(DEFAULT_MAX_CONTENT_LENGTH))
        .addLast(HttpChannelHandler())
    }
  }
  
  @Sharable
  inner class HttpChannelHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(context: ChannelHandlerContext, message: Any) {
      if(message is FullHttpRequest) {
        // wrap the message and create a new response
        val incoming = IncomingMessageIntrinsic(message)
        val outgoing = ServerResponseIntrinsic(incoming, context, message.protocolVersion())

        // invoke the guest listener
         requestListener(incoming, outgoing)
      } else {
        super.channelRead(context, message)
      }
    }
  }
  
  override var headersTimeout: Int = DEFAULT_HEADERS_TIMEOUT
  override var requestTimeout: Int = DEFAULT_REQUEST_TIMEOUT
  override var maxHeadersCount: Int = DEFAULT_MAX_HEADER_COUNT
  override var maxRequestsPerSocket: Int = DEFAULT_MAX_REQUEST_COUNT
  override var timeout: Int = DEFAULT_SOCKET_TIMEOUT
  override var keepAliveTimeout: Int = DEFAULT_KEEPALIVE_TIMEOUT
  
  override var listening: Boolean = false
  
  private lateinit var serverGroup: EventLoopGroup
  private lateinit var workerGroup: EventLoopGroup

  override fun listen(port: Int) {
    if(listening) return
    listening = true

    ServerBootstrap().apply {
      serverGroup = NioEventLoopGroup()
      workerGroup = NioEventLoopGroup()
      group(serverGroup, workerGroup)

      channel(NioServerSocketChannel::class.java)
      childHandler(HttpChannelInitializer())

      bind(port).sync()
    }
  }

  override fun setTimeout(msecs: Long, callback: Any?): Server {
    TODO("Not yet implemented")
  }

  override fun close(callback: Any?) {
    if(!listening) return

    listening = false
    TODO("Not yet implemented")
  }

  override fun closeAllConnections() {
    if(!listening) return

    TODO("Not yet implemented")
  }

  override fun closeIdleConnections() {
    if(!listening) return

    TODO("Not yet implemented")
  }

  companion object {
    const val DEFAULT_SOCKET_TIMEOUT = 0
    const val DEFAULT_HEADERS_TIMEOUT = 120_000
    const val DEFAULT_REQUEST_TIMEOUT = 120_000
    const val DEFAULT_KEEPALIVE_TIMEOUT = 0

    const val DEFAULT_MAX_REQUEST_COUNT = 0
    const val DEFAULT_MAX_HEADER_COUNT = 0
    const val DEFAULT_MAX_CONTENT_LENGTH = 1_048_576
  }
}