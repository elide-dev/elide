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
package elide.runtime.http.server.js.node

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertNotNull
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.netty.NettyCallHandlerAdapter
import elide.runtime.intrinsics.GuestIntrinsic

@MicronautTest(rebuildContext = true)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
internal class JsNodeHttpApplicationTest : AbstractJsTest() {
  lateinit var executor: ContextAwareExecutor

  @BeforeEach fun setup() {
    executor = ContextAwareExecutor(
      maxContextPoolSize = 1,
      baseExecutor = Executors.newCachedThreadPool(),
      contextFactory = {
        val context = engine.acquire().unwrap()
        val bindings = GuestIntrinsic.MutableIntrinsicBindings.Factory.create()
        intrinsicsManager().resolver().resolve(GraalVMGuest.JAVASCRIPT).forEach {
          it.install(bindings)
        }

        val contextBindings = context.getBindings("js")
        bindings.forEach { (symbol, value) -> contextBindings.putMember(symbol.symbol, value) }
        context
      },
    )
  }

  @AfterEach fun teardown() {
    executor.shutdownNow()
  }

  private fun ContextAwareExecutor.awaitContextTasks(channel: EmbeddedChannel) {
    val latch = submit { /* noop, just wait until the single context is available again */ }
    while (!latch.isDone) channel.runPendingTasks()
  }

  private fun testChannel(app: NodeHttpServerApplication): EmbeddedChannel {
    @Suppress("UNCHECKED_CAST")
    return EmbeddedChannel(
      NettyCallHandlerAdapter(
        application = app as HttpApplication<CallContext>,
        defaultServerName = "Elide/Test",
      ),
    )
  }

  private fun testApp(@Language("JavaScript") source: String): NodeHttpServerApplication {
    val entrypoint = Source.create("js", source)

    val app = object : NodeHttpServerApplication(executor) {
      override fun dispatch(request: NodeHttpServerRequest, response: NodeHttpServerResponse) {
        executor.execute {
          runCatching {
            val func = Context.getCurrent().eval(entrypoint)
            func.execute(request, response)
          }.onFailure {
            throw it
          }
        }
      }
    }

    return app
  }

  private fun EmbeddedChannel.assertResponse(
    request: HttpRequest = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"),
    requestContent: List<String> = emptyList(),
    expectedStatus: HttpResponseStatus = HttpResponseStatus.OK,
  ): HttpResponse {
    writeInbound(request)
    requestContent.forEach { content ->
      val buf = Unpooled.copiedBuffer(content, Charsets.UTF_8)
      writeInbound(DefaultHttpContent(buf))
    }

    writeInbound(LastHttpContent.EMPTY_LAST_CONTENT)
    runPendingTasks()

    while (!Thread.currentThread().isInterrupted) {
      executor.awaitContextTasks(this)
      runPendingTasks()

      readOutbound<Any>()?.let { response ->
        assertIs<HttpResponse>(response, "expected a response to be returned")
        assertEquals(expectedStatus, response.status())

        return response
      }

      Thread.sleep(10)
    }

    fail("expected a response to be received")
  }

  private fun EmbeddedChannel.assertResponseContent(vararg chunks: String) {
    for (chunk in chunks) {
      executor.awaitContextTasks(this)
      runPendingTasks()

      readOutbound<HttpContent>().let { content ->
        assertNotNull(content, "expected a content chunk")
        assertEquals(chunk, content.content().toString(Charsets.UTF_8))
        content.release()
      }
    }

    executor.awaitContextTasks(this)
    runPendingTasks()

    assertIs<LastHttpContent>(readOutbound<HttpContent>())
  }

  @Test fun `should handle calls`() {
    val app = testApp(
      """
      ((req, res) => {
          res.writeHead(200);
          res.end('Hello World');
      })
      """.trimIndent(),
    )

    val channel = testChannel(app)
    channel.assertResponse()
    channel.assertResponseContent("Hello World")
  }

  @Test fun `should read request properties`() {
    val app = testApp(
      """
      ((req, res) => {
          let message = '';
          message += req.method;
          message += req.url;
          message += req.headers['x-test-message'];
          
          res.writeHead(200);
          res.end(message);
      })
      """.trimIndent(),
    )

    val channel = testChannel(app)

    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test")
    request.headers().set("x-test-message", ":hello")
    channel.assertResponse(request)
    channel.assertResponseContent("GET/test:hello")
  }

  @Test fun `should read request body`() {
    val app = testApp(
      """
      ((req, res) => {
          let message = '';
          req.on('data', chunk => {
              message += chunk.toString('utf8')
          });
          req.on('end', () => {
            res.writeHead(200);
            res.end(message);
          })
      })
      """.trimIndent(),
    )

    val channel = testChannel(app)

    channel.assertResponse(requestContent = listOf("hello", " ", "world"))
    channel.assertResponseContent("hello world")
  }

  @Test fun `should write response body`() {
    val app = testApp(
      """
      ((req, res) => {
          res.writeHead(200);
          res.write('hello');
          res.end('world');
      })
      """.trimIndent(),
    )

    val channel = testChannel(app)

    channel.assertResponse()
    channel.assertResponseContent("hello", "world")
  }

  @Test fun `should set response headers`() {
    val app = testApp(
      """
      ((req, res) => {
          res.setHeader('x-test-header', 42);
          res.setHeader('x-test-array', ['3', '14'])
          res.writeHead(200, { 'Content-Length': '5', 'Content-Type': 'text/plain' });
          res.end('hello');
      })
      """.trimIndent(),
    )

    val channel = testChannel(app)

    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test")
    request.headers().set("x-test-message", ":hello")

    val response = channel.assertResponse(request)
    channel.assertResponseContent("hello")

    assertEquals("42", response.headers()["x-test-header"])
    assertContentEquals(listOf("3", "14"), response.headers().getAll("x-test-array"))
    assertEquals("text/plain", response.headers()[HttpHeaderNames.CONTENT_TYPE])
    assertEquals("5", response.headers()[HttpHeaderNames.CONTENT_LENGTH])
  }
}
