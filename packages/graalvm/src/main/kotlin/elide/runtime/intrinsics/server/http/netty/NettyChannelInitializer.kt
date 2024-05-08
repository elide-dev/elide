/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpDecoderConfig
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import elide.runtime.core.DelicateElideApi

/** Initializer used to configure a custom pipeline with an HTTP codec and a shared [handler]. */
@DelicateElideApi internal class NettyChannelInitializer(
  private val handler: NettyRequestHandler
) : ChannelInitializer<SocketChannel>() {
  /** Configure a new [HttpResponseEncoder] to be added to the channel pipeline. */
  private fun createHttpEncoder() = HttpResponseEncoder()

  /** Configure a new [HttpRequestDecoder] to be added to the channel pipeline. */
  private fun createHttpDecoder() = HttpRequestDecoder(
    HttpDecoderConfig().apply {
      maxInitialLineLength = MAX_INITIAL_LINE_LENGTH
      maxHeaderSize = MAX_HEADER_SIZE
      maxChunkSize = MAX_CHUNK_SIZE
    }
  )

  override fun initChannel(channel: SocketChannel) {
    channel.pipeline()
      .addLast(ENCODER_PIPELINE_KEY, createHttpEncoder())
      .addLast(DECODER_PIPELINE_KEY, createHttpDecoder())
      .addLast(HANDLER_PIPELINE_KEY, handler)
  }

  private companion object {
    /** Name used for the HTTP encoder in the channel pipeline. */
    private const val ENCODER_PIPELINE_KEY = "encoder"

    /** Name used for the HTTP decoder in the channel pipeline. */
    private const val DECODER_PIPELINE_KEY = "decoder"

    /** Name used for the request handler in the channel pipeline. */
    private const val HANDLER_PIPELINE_KEY = "handler"

    /** Maximum initial line length (in bytes) allowed by the request decoder. */
    private const val MAX_INITIAL_LINE_LENGTH = 4096

    /** Maximum header length (in bytes) allowed by the request decoder. */
    private const val MAX_HEADER_SIZE = 8192

    /** Maximum chunk length (in bytes) allowed by the request decoder. */
    private const val MAX_CHUNK_SIZE = 8192
  }
}
