package elide.runtime.intriniscs.server.http.netty

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import elide.runtime.core.DelicateElideApi

@DelicateElideApi internal class NettyChannelInitializer(
  private val handler: NettyRequestHandler
) : ChannelInitializer<SocketChannel>() {
  private fun createHttpEncoder() = HttpResponseEncoder()

  private fun createHttpDecoder() = HttpRequestDecoder(
    /* maxInitialLineLength = */ MAX_INITIAL_LINE_LENGTH,
    /* maxHeaderSize = */ MAX_HEADER_SIZE,
    /* maxChunkSize = */  MAX_CHUNK_SIZE,
    /* validateHeaders = */VALIDATE_HEADERS,
  )

  override fun initChannel(channel: SocketChannel) {
    channel.pipeline()
      .addLast(ENCODER_PIPELINE_KEY, createHttpEncoder())
      .addLast(DECODER_PIPELINE_KEY, createHttpDecoder())
      .addLast(HANDLER_PIPELINE_KEY, handler)
  }

  private companion object {
    private const val ENCODER_PIPELINE_KEY = "encoder"
    private const val DECODER_PIPELINE_KEY = "decoder"
    private const val HANDLER_PIPELINE_KEY = "handler"

    private const val MAX_INITIAL_LINE_LENGTH = 4096
    private const val MAX_HEADER_SIZE = 8192
    private const val MAX_CHUNK_SIZE = 8192
    private const val VALIDATE_HEADERS = false
  }
}