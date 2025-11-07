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
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class NettyCallHandlerAdapterTest : AbstractNettyCallHandlerTest() {
  private lateinit var handler: NettyCallHandlerAdapter
  private lateinit var application: TestApplication
  private lateinit var channel: EmbeddedChannel
  private val call: HttpCall<TestContext> get() = application.call

  @BeforeEach fun setup() {
    application = TestApplication()

    @Suppress("UNCHECKED_CAST")
    handler = NettyCallHandlerAdapter(application as HttpApplication<CallContext>, SERVER_NAME)
    channel = EmbeddedChannel(handler)
  }

  @AfterEach fun teardown() {
    channel.close().get()
  }

  @Test fun `should dispatch a call for incoming requests`() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test").apply {
      headers().set("x-elide-test", "request")
    }

    channel.writeInbound(request)
    channel.writeInbound(LastHttpContent.EMPTY_LAST_CONTENT)
    channel.runPendingTasks()

    assertNotNull(call, "expected call to be handled by application")

    assertEquals(HttpVersion.HTTP_1_1, call.request.protocolVersion())
    assertEquals(HttpMethod.POST, call.request.method())
    assertEquals("/test", call.request.uri())
    assertEquals("request", call.request.headers().get("x-elide-test"))

    call.response.status = HttpResponseStatus.TOO_MANY_REQUESTS
    call.response.setHeader("x-elide-test", "response")
    call.send()

    channel.runPendingTasks()
    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response to be sent")
      assertEquals(HttpVersion.HTTP_1_1, response.protocolVersion())
      assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, response.status())
      assertEquals("response", response.headers().get("x-elide-test"))
    }

    channel.runPendingTasks()
    channel.readOutbound<HttpContent>().let { content ->
      assertNotNull(content, "expected empty content to be sent")
      assertIs<LastHttpContent>(content)
    }
  }

  @Test fun `should send headers automatically before body`() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    channel.writeInbound(request)
    channel.runPendingTasks()

    call.send()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent automatically")
      assertEquals(HttpResponseStatus.OK, response.status())
    }

    channel.runPendingTasks()
    channel.readOutbound<HttpContent>().let { content ->
      assertNotNull(content, "expected empty content to be sent")
      assertIs<LastHttpContent>(content)
    }
  }

  @Test fun `should not send body automatically after headers`() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    channel.writeInbound(request)
    channel.runPendingTasks()

    call.sendHeaders()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent automatically")
      assertEquals(HttpResponseStatus.OK, response.status())
    }

    channel.runPendingTasks()
    assertTrue(channel.outboundMessages().isEmpty(), "expected outbound messages to be empty")
  }

  @Test fun `should discard remaining request body after closed`() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    channel.writeInbound(request)
    channel.runPendingTasks()

    call.send()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent automatically")
      assertEquals(HttpResponseStatus.OK, response.status())
    }

    val data = DefaultHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8))
    channel.writeInbound(data)
    channel.runPendingTasks()
    assertEquals(0, data.refCnt(), "expected incoming data to be discarded after closing")
  }

  @Test fun `should call application context lifecycle callbacks`() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    channel.writeInbound(request)
    channel.runPendingTasks()

    assertFalse(
      with(call.context) { headersSent || contentFlushed || contentSent || callEnded },
      "expected context lifecycle events to not be called",
    )

    call.sendHeaders()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent automatically")
      assertEquals(HttpResponseStatus.OK, response.status())
      assertTrue(call.context.headersSent)
    }

    call.send()
    channel.runPendingTasks()

    assertTrue(call.context.contentFlushed)
    assertIs<LastHttpContent>(channel.readOutbound<HttpContent>(), "expected EOS marker")

    assertTrue(call.context.contentSent)
    assertTrue(call.context.callEnded)
  }

  @Test fun `should respond with 500 when call handler throws`() {
    application.onHandle = { error("synthetic failure") }

    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected error response to be sent")
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
      assertTrue(call.context.callEnded)
    }
  }

  @Test fun `should accept more requests after error`() {
    application.onHandle = { error("synthetic failure") }

    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected error response to be sent")
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
      assertTrue(call.context.callEnded)
    }

    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected second error response to be sent")
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
      assertTrue(call.context.callEnded)
    }
  }

  @Test fun `should send 500 response when fail called before headers`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    val failure = IllegalStateException("synthetic failure")
    call.fail(failure)
    channel.runPendingTasks()

    channel.readOutbound<FullHttpResponse>().let { response ->
      assertNotNull(response, "expected error response to be sent")
      assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
      assertEquals("close", response.headers().get(HttpHeaderNames.CONNECTION))
    }

    assertTrue(call.context.callEnded)
    assertSame(failure, call.context.error)
  }

  @Test fun `should close connection when fail called after headers sent`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    call.sendHeaders()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent")
      assertEquals(HttpResponseStatus.OK, response.status())
    }

    assertTrue(channel.isOpen)
    val failure = IllegalStateException("synthetic failure")
    call.fail(failure)
    channel.runPendingTasks()

    assertFalse(channel.isOpen)
    assertTrue(call.context.callEnded)
    assertSame(failure, call.context.error)
  }

  @Test fun `should close connection if call fails after headers sent`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    call.context.onNextEvent = { error("synthetic failure") }
    call.sendHeaders()

    channel.runPendingTasks()
    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected error response to be sent")
      assertEquals(HttpResponseStatus.OK, response.status())
    }

    assertFalse(channel.isOpen)
  }

  @Test fun `should allow reading streamed request body`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    var message = ""
    application.call.requestBody.consume { buf, reader ->
      message += buf.toString(Charsets.UTF_8)
      reader.pull()
    }

    channel.runPendingTasks()
    channel.writeInbound(DefaultHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8)))
    channel.writeInbound(DefaultHttpContent(Unpooled.copiedBuffer(" ", Charsets.UTF_8)))
    channel.writeInbound(DefaultLastHttpContent(Unpooled.copiedBuffer("world", Charsets.UTF_8)))

    channel.runPendingTasks()
    assertEquals("hello world", message)
  }

  @Test fun `should discard content after call until next request`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    channel.writeInbound(DefaultLastHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8)))
    channel.runPendingTasks()

    call.send()
    channel.runPendingTasks()

    val startData = DefaultHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8))
    val endData = DefaultLastHttpContent(Unpooled.copiedBuffer("world", Charsets.UTF_8))

    channel.writeInbound(startData)
    channel.writeInbound(endData)
    channel.runPendingTasks()

    assertEquals(0, startData.refCnt())
    assertEquals(0, endData.refCnt())

    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test2"))
    channel.runPendingTasks()

    assertEquals("/test2", application.call.request.uri())
  }

  @Test fun `should discard content after request body is closed`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    // discard immediately
    call.requestBody.consume(onAttached = { it.release() }) { _, _ -> fail("Expected content to be discarded") }
    channel.runPendingTasks()

    val startData = DefaultHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8))
    val endData = DefaultLastHttpContent(Unpooled.copiedBuffer("world", Charsets.UTF_8))
    channel.writeInbound(startData)
    channel.writeInbound(endData)
    channel.runPendingTasks()

    assertEquals(0, startData.refCnt())
    assertEquals(0, endData.refCnt())

    call.send()
    channel.runPendingTasks()

    assertTrue(call.context.callEnded)
    assertNull(call.context.error)
  }

  @Test fun `should allow writing streamed response body`() {
    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.runPendingTasks()

    var counter = 0
    application.call.responseBody.source { writer ->
      counter++
      writer.write(Unpooled.copiedBuffer("hello$counter", Charsets.UTF_8))
      if (counter == 2) writer.end()
    }

    application.call.send()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent")
      assertEquals(HttpResponseStatus.OK, response.status())
    }

    channel.readOutbound<HttpContent>().let { chunk ->
      assertNotNull(chunk, "expected content chunk to be sent")
      assertEquals("hello1", chunk.content().toString(Charsets.UTF_8))
    }

    channel.readOutbound<HttpContent>().let { chunk ->
      assertNotNull(chunk, "expected content chunk to be sent")
      assertEquals("hello2", chunk.content().toString(Charsets.UTF_8))
    }

    channel.readOutbound<HttpContent>().let { chunk ->
      assertNotNull(chunk, "expected final content chunk to be sent")
      assertIs<LastHttpContent>(chunk)
    }
  }

  @Test fun `should set server name header by default`() {
    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    channel.writeInbound(request)
    channel.runPendingTasks()

    call.send()
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected response header to be sent automatically")
      assertEquals(HttpResponseStatus.OK, response.status())
      assertEquals(SERVER_NAME, response.headers().get(HttpHeaderNames.SERVER))
    }

    channel.runPendingTasks()
    channel.readOutbound<HttpContent>().let { content ->
      assertNotNull(content, "expected empty content to be sent")
      assertIs<LastHttpContent>(content)
    }
  }

  private companion object {
    const val SERVER_NAME = "Elide/Test"
  }
}
