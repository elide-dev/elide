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

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import io.netty.util.ReferenceCountUtil
import kotlin.test.*
import elide.runtime.intrinsics.server.http.v2.*

internal class NettyHttpContextAdapterTest {
  private inline fun testAdapter(
    block: (channel: EmbeddedChannel, handler: MockContextHandler, contextTracker: MockHttpContext.Factory) -> Unit
  ) {
    val handler = MockContextHandler()
    val contextTracker = MockHttpContext.Factory()
    val channel = EmbeddedChannel(NettyHttpContextAdapter(contextTracker, handler))

    block(channel, handler, contextTracker)
  }

  private inline fun <T : HttpContextHandler> testAdapter(
    createHandler: () -> T,
    block: (channel: EmbeddedChannel, handler: T, contextTracker: MockHttpContext.Factory) -> Unit
  ) {
    val handler = createHandler()
    val contextTracker = MockHttpContext.Factory()
    val channel = EmbeddedChannel(NettyHttpContextAdapter(contextTracker, handler))

    block(channel, handler, contextTracker)
  }


  @Test fun `should invoke context handler`() = testAdapter { channel, handler, _ ->
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/payload")
    channel.writeInbound(request)
    val context = handler.assertContextOpen()

    val collector = CollectingConsumer<String> { it.content().toString(CharsetUtil.UTF_8) }
    context.requestBody.sink(collector)

    val chunk = DefaultHttpContent(Unpooled.copiedBuffer("hello", CharsetUtil.UTF_8))
    val last = DefaultLastHttpContent(Unpooled.copiedBuffer("world", CharsetUtil.UTF_8))

    channel.writeInbound(chunk)
    channel.writeInbound(last)

    channel.runPendingTasks()
    channel.checkException()

    val received = collector.collect()
    assertEquals(listOf("hello", "world"), received)

    // No response should be written until the handler finishes.
    assertNull(channel.readOutbound<HttpResponse>())

    channel.finishAndReleaseAll()
  }

  @Test fun `should flush response when handler completes`() = testAdapter { channel, handler, _ ->
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/response")
    channel.writeInbound(request)

    val context = handler.assertContextOpen()
    assertNull(channel.readOutbound())

    val responseData = listOf(
      DefaultHttpContent(Unpooled.copiedBuffer("hello", CharsetUtil.UTF_8)),
      DefaultLastHttpContent(Unpooled.copiedBuffer("world", CharsetUtil.UTF_8)),
    )

    val producer = CollectionProducer(responseData)
    context.responseBody.source(producer)

    handler.sendResponse()

    channel.runPendingTasks()
    channel.checkException()

    val outbound = channel.readOutbound<HttpResponse>()
    assertNotNull(outbound)
    assertEquals(HttpResponseStatus.OK, outbound.status())

    assertEquals(responseData[0], channel.readOutbound<HttpContent>())
    assertEquals(responseData[1], channel.readOutbound<HttpContent>())

    assertNull(channel.readOutbound<HttpContent>())

    channel.finishAndReleaseAll()
  }

  @Test fun `should respond to unsupported expectation and skip content`() = testAdapter { channel, handler, contexts ->
    val request: HttpRequest = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/reject").apply {
      headers()[HttpHeaderNames.EXPECT] = "something-else"
    }

    channel.writeInbound(request)

    contexts.assertNoContext()
    handler.assertNoContext()

    val response = channel.readOutbound<FullHttpResponse>()
    assertNotNull(response)
    assertEquals(HttpResponseStatus.EXPECTATION_FAILED, response.status())
    ReferenceCountUtil.release(response)

    val chunk = DefaultHttpContent(Unpooled.copiedBuffer("ignored", CharsetUtil.UTF_8))
    val last = DefaultLastHttpContent(Unpooled.EMPTY_BUFFER)

    channel.writeInbound(chunk)
    channel.writeInbound(last)

    assertEquals(0, chunk.refCnt())

    channel.finishAndReleaseAll()
  }

  @Test fun `should emit continue and pass request to handler`() = testAdapter { channel, handler, contexts ->
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/continue").apply {
      headers().set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE)
    }

    channel.writeInbound(request)

    val continueResponse = channel.readOutbound<FullHttpResponse>()
    assertNotNull(continueResponse)
    assertEquals(HttpResponseStatus.CONTINUE, continueResponse.status())
    ReferenceCountUtil.release(continueResponse)

    handler.assertContextOpen()
    contexts.assertContextOpen()

    assertNull(request.headers().get(HttpHeaderNames.EXPECT))

    handler.sendResponse()
    channel.runPendingTasks()

    val response = channel.readOutbound<HttpResponse>()
    assertNotNull(response)
    assertEquals(HttpResponseStatus.OK, response.status())

    channel.finishAndReleaseAll()
  }

  @Test fun `should emit 500 response on failed handler promise`() = testAdapter { channel, handler, _ ->
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/payload")
    channel.writeInbound(request)

    handler.assertContextOpen()
    handler.setFailed(RuntimeException("test handler failure"))

    val response = channel.readOutbound<HttpResponse>()
    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
  }

  @Test fun `should emit 500 response on crashed handler`() {
    testAdapter(
      createHandler = { HttpContextHandler { _, _ -> fail("expected exception to be caught") } },
    ) { channel, _, contextTracker ->
      val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/payload")
      channel.writeInbound(request)

      contextTracker.assertContextOpen()

      val response = channel.readOutbound<HttpResponse>()
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
    }
  }
}
