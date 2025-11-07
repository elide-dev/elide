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
package elide.runtime.http.server.netty

import elide.runtime.http.server.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class NettyCallHandlerAdapterE2ETest : AbstractNettyCallHandlerTest() {
  private lateinit var handler: NettyCallHandlerAdapter
  private lateinit var application: TestApplication
  private lateinit var serverChannel: EmbeddedChannel

  private lateinit var clientChannel: EmbeddedChannel
  private lateinit var clientMessages: MutableList<Any>

  @BeforeEach fun setup() {
    application = TestApplication()

    @Suppress("UNCHECKED_CAST")
    handler = NettyCallHandlerAdapter(application as HttpApplication<CallContext>, "Elide/Test")

    serverChannel = EmbeddedChannel(
      HttpServerCodec(),
      HttpServerExpectContinueHandler(),
      handler,
    )

    clientMessages = mutableListOf()
    val collectorHandler = object : ChannelInboundHandlerAdapter() {
      override fun channelRead(ctx: ChannelHandlerContext?, msg: Any) {
        clientMessages.add(msg)
      }
    }

    clientChannel = EmbeddedChannel(HttpClientCodec(), collectorHandler)
  }

  @AfterEach fun teardown() {
    serverChannel.close().get()
  }

  private fun transferClientToServer() {
    while (true) clientChannel.readOutbound<Any>()?.let { serverChannel.writeInbound(it) } ?: break
  }

  private fun transferServerToClient() {
    while (true) {
      serverChannel.readOutbound<Any>()?.let {
        clientChannel.writeInbound(it)
      } ?: break
    }
  }

  @Test fun `should handle call`() {
    // prepare handling
    application.onHandle = { call ->
      call.responseBody.source { writer ->
        writer.write("hello")
        writer.end()
      }

      call.response.status = HttpResponseStatus.TOO_MANY_REQUESTS
      call.response.setHeader("x-elide-test", "true")
      call.response.setHeader(HttpHeaderNames.CONTENT_LENGTH, 5)
      call.send()
    }

    // send request
    clientChannel.writeAndFlush(DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test"))
    transferClientToServer()

    // run handling
    serverChannel.runPendingTasks()
    transferServerToClient()

    // receive and check response
    clientChannel.runPendingTasks()
    clientMessages.removeFirstOrNull().let {
      assertIs<HttpResponse>(it, "expected a response header")
      assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, it.status())
      assertEquals(HttpVersion.HTTP_1_1, it.protocolVersion())
      assertEquals("true", it.headers().get("x-elide-test"))
      assertEquals(5, it.headers().getInt(HttpHeaderNames.CONTENT_LENGTH))
    }

    clientMessages.removeFirstOrNull().let {
      assertIs<LastHttpContent>(it, "expected a single content message")
      assertEquals("hello", it.content().toString(Charsets.UTF_8))
      it.release()
    }
  }

  @Test fun `should process multiple requests`() {
    application.onHandle = { it.send() } // send the default response

    repeat(3) {
      val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test#${it + 1}")
      clientChannel.writeAndFlush(request)
    }

    transferClientToServer()
    serverChannel.runPendingTasks()
    transferServerToClient()

    repeat(3) {
      val response = clientMessages.removeFirstOrNull()
      assertIs<HttpResponse>(response, "expected a response for request #${it + 1}")
      assertEquals(HttpResponseStatus.OK, response.status())

      val body = clientMessages.removeFirstOrNull()
      assertIs<LastHttpContent>(body, "expected an empty body (request #${it + 1})")
    }
  }

  @Test fun `should support streamed calls`() {
    var call: HttpCall<TestContext>? = null
    var requestReader: ReadableContentStream.Reader? = null
    var responseWriter: WritableContentStream.Writer? = null
    val requestChunks = mutableListOf<ByteBuf>()

    application.onHandle = { incoming ->
      call = incoming

      call.response.setHeader(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
      call.responseBody.source(onAttached = { responseWriter = it }) {}
      call.requestBody.consume(onAttached = { requestReader = it }) { buf, _ -> requestChunks.add(buf.retain()) }
      call.send()
    }

    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    request.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")

    clientChannel.writeAndFlush(request)
    transferClientToServer()
    serverChannel.runPendingTasks()

    // call handled
    assertTrue(requestChunks.isEmpty())
    requestReader!!.pull()
    serverChannel.runPendingTasks()

    // response header received
    transferServerToClient()
    clientMessages.removeFirstOrNull().let {
      assertIs<HttpResponse>(it, "expected a response header")
      assertEquals(HttpResponseStatus.OK, it.status())
    }

    // client sends a chunk
    clientChannel.writeAndFlush(DefaultHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8)))
    transferClientToServer()
    serverChannel.runPendingTasks()

    // server reads one chunk
    requestChunks.removeFirstOrNull().let {
      assertIs<ByteBuf>(it, "expected a request content chunk")
      assertEquals("hello", it.toString(Charsets.UTF_8))
      it.release()
    }

    // server sends one chunk
    responseWriter!!.write("olleh")
    serverChannel.runPendingTasks()
    transferServerToClient()

    // client reads one chunk
    clientMessages.removeFirstOrNull().let {
      assertIs<HttpContent>(it, "expected a response content chunk")
      assertIsNot<LastHttpContent>(it, "expected chunk to not be the last")
      assertEquals("olleh", it.content().toString(Charsets.UTF_8))
      it.release()
    }

    // client sends final chunk
    clientChannel.writeAndFlush(DefaultLastHttpContent(Unpooled.copiedBuffer("world", Charsets.UTF_8)))
    transferClientToServer()
    serverChannel.runPendingTasks()

    // server reads final chunk
    requestReader.pull()
    serverChannel.runPendingTasks()

    requestChunks.removeFirstOrNull().let {
      assertIs<ByteBuf>(it, "expected another response content chunk")
      assertEquals("world", it.toString(Charsets.UTF_8))
      it.release()
    }

    // server responds with final chunk
    responseWriter.write("dlrow")
    serverChannel.runPendingTasks()
    transferServerToClient()

    // client reads final chunk
    clientMessages.removeFirstOrNull().let {
      assertIs<HttpContent>(it, "expected a response content chunk")
      assertIsNot<LastHttpContent>(it, "expected chunk to not be the last")
      assertEquals("dlrow", it.content().toString(Charsets.UTF_8))
      it.release()
    }

    // server signals end of response
    responseWriter.end()
    serverChannel.runPendingTasks()
    transferServerToClient()

    clientMessages.removeFirstOrNull().let {
      assertIs<LastHttpContent>(it, "expected end of response marker")
      assertEquals(0, it.content().readableBytes())
      it.release()
    }

    // call is now closed
    serverChannel.runPendingTasks()
    assertTrue(call!!.context.callEnded)
    assertTrue(requestChunks.isEmpty())
  }

  @Test fun `should return error response on handler exception`() {
    application.onHandle = { error("test exception") }

    clientChannel.writeAndFlush(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    transferClientToServer()
    serverChannel.runPendingTasks()
    transferServerToClient()

    clientMessages.removeFirstOrNull().let {
      assertIs<HttpResponse>(it, "expected a response")
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, it.status())
    }

    clientMessages.removeFirstOrNull().let {
      assertIs<LastHttpContent>(it, "expected an end of response marker")
      assertEquals(0, it.content().readableBytes(), "expected response to be empty")
    }
  }
}
