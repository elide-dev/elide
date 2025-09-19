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

package elide.runtime.intrinsics.server.http.v2.channels

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.intrinsics.server.http.v2.*
import elide.runtime.intrinsics.server.http.v2.HttpRequest
import elide.runtime.intrinsics.server.http.v2.HttpResponse
import elide.testing.annotations.Test

private class TestHttpContextFactory : HttpContextFactory<HttpContext>() {
  override fun newContext(
    incomingRequest: NettyHttpRequest,
    channelContext: ChannelHandlerContext,
    requestSource: HttpContentSource,
    responseSink: HttpContentSink
  ): HttpContext {
    val request = object : HttpRequest(
      httpVersion = incomingRequest.protocolVersion(),
      method = incomingRequest.method(),
      uri = incomingRequest.uri(),
      headers = incomingRequest.headers(),
    ) {
      override val body: HttpContentSource = requestSource
    }

    val response = object : HttpResponse(
      httpVersion = incomingRequest.protocolVersion(),
      status = HttpResponseStatus.OK,
      headers = DefaultHttpHeadersFactory.headersFactory().newEmptyHeaders(),
    ) {
      override val body: HttpContentSink = responseSink
    }

    return object : HttpContext() {
      override val request: HttpRequest = request
      override val response: HttpResponse = response
      override val session: HttpSession = HttpSession()
      override fun close() {
        channelContext.close()
      }
    }
  }
}

class HandlerTest {
  @Test fun `should handle http request`() {
    val pipeline = object : HttpHandlerPipeline() {
      override fun handle(httpContext: HttpContext, channelContext: ChannelHandlerContext): ChannelFuture {
        val completion = channelContext.newPromise()
        val digest = MessageDigest.getInstance("MD5")

        // stream the request and progressively hash its body
        httpContext.request.body?.sink(
          object : HttpContentSource.Consumer {
            override fun consume(content: HttpContent, handle: HttpContentSource.Handle) {
              println("Incoming request content chunk")
              digest.update(content.content().nioBuffer())
              content.release()

              handle.pull()
            }

            override fun released() {
              println("Done reading request, will prepare response")

              // prepare the response
              httpContext.response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
              httpContext.response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream")
              httpContext.response.headers().set(HttpHeaderNames.SERVER, "elide-http-v2")

              val hash = digest.digest()
              val counter = AtomicInteger(0)

              httpContext.response.body.source(
                object : HttpContentSink.Producer {
                  override fun pull(handle: HttpContentSink.Handle) {
                    val data = channelContext.alloc().buffer(hash.size)
                    data.writeBytes(hash)

                    val counter = counter.getAndIncrement()
                    if (counter < 4) {
                      println("Sending response chunk")
                      handle.push(DefaultHttpContent(data))
                    } else if (counter == 4) {
                      println("Sending final response chunk")
                      // we're finished with the content
                      handle.push(DefaultLastHttpContent(data))
                      handle.release(close = true)
                    }
                  }
                },
              )

              // we can now send the response
              println("Handler done, sending response")
              completion.setSuccess()
            }
          },
        )

        // stream the response
        println("Handler ready")
        return completion
      }
    }

    val decoderConfig = HttpDecoderConfig().apply {
      maxChunkSize = 5
    }

    val bossGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    val workerGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())

    val bootstrap = ServerBootstrap().apply {
      group(bossGroup, workerGroup)
      channel(NioServerSocketChannel::class.java)
      childHandler(
        object : ChannelInitializer<SocketChannel>() {
          override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(
              HttpServerCodec(decoderConfig),
              NettyHttpContextAdapter(TestHttpContextFactory(), pipeline),
            )
          }
        },
      )

      option(ChannelOption.SO_BACKLOG, 128)
      option(ChannelOption.SO_KEEPALIVE, true)
      // option(ChannelOption.AUTO_READ, false)
    }

    val future = bootstrap.bind(9019).sync()
    println("Test server started on ${future.channel().remoteAddress()}")
    future.channel().closeFuture().sync()

    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }
}
