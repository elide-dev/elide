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

package elide.runtime.http.server.js.worker

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Source
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertNotNull
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.HttpCleartextService
import elide.runtime.http.server.netty.NettyCallHandlerAdapter
import elide.runtime.intrinsics.GuestIntrinsic

@MicronautTest(rebuildContext = true)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
internal class JsWorkerApplicationTest : AbstractJsTest() {
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

  private fun testStack(): HttpApplicationStack {
    val cleartextBinding = HttpApplicationStack.ServiceBinding(
      address = InetSocketAddress("localhost", 8080),
      scheme = HttpCleartextService.SCHEME,
    )

    return HttpApplicationStack(
      services = listOf(
        HttpApplicationStack.Service(HttpCleartextService.LABEL, Result.success(cleartextBinding)),
      ),
      channels = emptyList(),
      groups = emptyList(),
    )
  }

  private fun testChannel(app: JsWorkerApplication): EmbeddedChannel {
    @Suppress("UNCHECKED_CAST")
    return EmbeddedChannel(
      NettyCallHandlerAdapter(
        application = app as HttpApplication<CallContext>,
        defaultServerName = "Elide/Test",
      ),
    )
  }

  private fun testApp(
    @Language("JavaScript") source: String,
  ): JsWorkerApplication {
    val entrypoint = Source.newBuilder("js", source, "app.mjs")
      .build()

    return JsWorkerApplication(entrypoint, executor)
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

    executor.awaitContextTasks(this)
    runPendingTasks()

    return readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected a response to be returned")
      assertEquals(expectedStatus, response.status())

      response
    }
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

  @Test fun `should serve request`() {
    val app = testApp(
      """
      export default async function fetch() {      
          return new Response("Hello World!", {
           status: 418,
           statusText: "I'm a teapot",
           headers: { "x-test-value": "42" }
          })
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    val response = channel.assertResponse(expectedStatus = HttpResponseStatus.valueOf(418, "I'm a teapot"))
    assertEquals(response.headers().get("x-test-value"), "42")

    channel.assertResponseContent("Hello World!")
  }

  @Test fun `should allow setting headers instance`() {
    val app = testApp(
      """
      export default async function fetch() {
          const headers = new Headers()
          headers.set("x-test-value", "42")

          return new Response("Hello World!", { headers })
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    val response = channel.assertResponse(expectedStatus = HttpResponseStatus.OK)
    assertEquals(response.headers().get("x-test-value"), "42")

    channel.assertResponseContent("Hello World!")
  }

  @Test fun `should encode objects as JSON`() {
    val app = testApp(
      """
      export default async function fetch() {      
          return new Response({ "hello": ["world", 42] })
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    channel.assertResponse(expectedStatus = HttpResponseStatus.OK)
    channel.assertResponseContent("{\"hello\":[\"world\",42]}")
  }

  @Test fun `should allow array views as response body`() {
    val app = testApp(
      """
      export default async function fetch() {
          return new Response(new Uint8Array([72, 101,108,108,111,33]))
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    channel.assertResponse(expectedStatus = HttpResponseStatus.OK)
    channel.assertResponseContent("Hello!")
  }

  @Test fun `should allow array buffers as response body`() {
    val app = testApp(
      """
      export default async function fetch() {
          return new Response(new Uint8Array([72, 101,108,108,111,33]).buffer)
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    channel.assertResponse(expectedStatus = HttpResponseStatus.OK)
    channel.assertResponseContent("Hello!")
  }

  @Test fun `should allow reading request content`() {
    val app = testApp(
      """
      export default async function fetch(request) {      
          return new Response(request.body)
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    channel.assertResponse(requestContent = listOf("Hello World!"), expectedStatus = HttpResponseStatus.OK)
    executor.awaitContextTasks(channel)
    channel.assertResponseContent("Hello World!")
  }

  @Test fun `should allow reading request properties`() {
    val app = testApp(
      """
      export default async function fetch(request) {
          let content = ""
          content += request.method
          content += ","
      
          content += request.url
          content += ","
      
          content += request.headers["x-test-header"]
      
          return new Response(content)
      }
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test")
    request.headers().set("x-test-header", "test")

    channel.assertResponse(request, expectedStatus = HttpResponseStatus.OK)
    channel.assertResponseContent("GET,http://localhost:8080/test,test")
  }
}
