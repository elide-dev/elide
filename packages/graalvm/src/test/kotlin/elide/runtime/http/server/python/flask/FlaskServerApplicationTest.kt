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
package elide.runtime.http.server.python.flask

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
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail
import elide.annotations.Inject
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeExecutor
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.gvm.python.PythonTest
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.HttpServerEngine
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.HttpApplicationStack.Service
import elide.runtime.http.server.netty.HttpApplicationStack.ServiceBinding
import elide.runtime.http.server.netty.HttpCleartextService
import elide.runtime.http.server.netty.NettyCallHandlerAdapter

@MicronautTest(rebuildContext = true)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class FlaskServerApplicationTest : PythonTest() {
  @Inject lateinit var runtimeExecutor: RuntimeExecutor
  @Inject lateinit var serverEngine: HttpServerEngine
  @Inject lateinit var entrypoint: EntrypointRegistry

  @TempDir lateinit var syntheticRoot: Path
  lateinit var executor: ContextAwareExecutor

  @BeforeEach fun setup() {
    executor = ContextAwareExecutor(
      maxContextPoolSize = 1,
      baseExecutor = Executors.newCachedThreadPool(),
      contextFactory = { engine.acquire().unwrap() },
    )

    runtimeExecutor.register(executor)
  }

  @AfterEach fun teardown() {
    executor.shutdown()
  }

  private fun testChannel(app: FlaskServerApplication): EmbeddedChannel {
    @Suppress("UNCHECKED_CAST")
    return EmbeddedChannel(
      NettyCallHandlerAdapter(
        application = app as HttpApplication<CallContext>,
        defaultServerName = "Elide/Test",
      ),
    )
  }

  private fun setupTestApplication(@Language("python") source: String): EmbeddedChannel {
    val sourcePath = syntheticRoot.resolve("app.py")
    sourcePath.writeText(source)

    val source = Source.newBuilder("python", sourcePath.toFile()).build()
    entrypoint.record(source)
    val channel = CompletableFuture<EmbeddedChannel>()

    serverEngine.use { application, _ ->
      channel.complete(testChannel(application as FlaskServerApplication))

      val cleartextBinding = ServiceBinding(
        address = InetSocketAddress("localhost", 8080),
        scheme = HttpCleartextService.SCHEME,
      )

      HttpApplicationStack(
        services = listOf(Service(HttpCleartextService.LABEL, Result.success(cleartextBinding))),
        channels = emptyList(),
        groups = emptyList(),
      )
    }

    executor.execute { Context.getCurrent().eval(source) }
    return channel.get()
  }

  private fun ContextAwareExecutor.awaitContextTasks(channel: EmbeddedChannel) {
    val latch = submit { /* noop, just wait until the single context is available again */ }
    while (!latch.isDone) channel.runPendingTasks()
  }

  private fun EmbeddedChannel.assertResponse(
    request: HttpRequest = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"),
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

    readOutbound<HttpContent>()?.let { assertIs<LastHttpContent>(it) }
  }

  @Test fun `should handle requests`() {
    val channel = setupTestApplication(
      """
      from elide import Flask, request

      # create and configure an app
      app = Flask(__name__)

      # basic app routing
      @app.route("/test")
      def hello_world():
          return f"{request.method},{request.path},{request.headers['X-Message']},{request.args['m']}"
          
      app.bind() # manual binding is required during tests
      """.trimIndent(),
    )

    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test?m=world")
    request.headers().set("X-Message", "hello")

    channel.assertResponse(request)

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.assertResponseContent("GET,/test?m=world,hello,world")
  }

  @Test fun `should support response tuples`() {
    val channel = setupTestApplication(
      """
      from elide import Flask

      app = Flask(__name__)

      @app.route("/")
      def handler():
          return ("created", 201, {"X-Custom": "true"})

      app.bind()
      """.trimIndent(),
    )

    val response = channel.assertResponse(expectedStatus = HttpResponseStatus.CREATED)
    assertEquals("true", response.headers().get("X-Custom"))

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.assertResponseContent("created")
  }

  @Test fun `should encode lists as JSON`() {
    val channel = setupTestApplication(
      """
      from elide import Flask

      app = Flask(__name__)

      @app.route("/")
      def index():
          return ["alpha", {"beta": 2}]

      app.bind()
      """.trimIndent(),
    )

    val response = channel.assertResponse()
    assertEquals("application/json; charset=utf-8", response.headers().get(HttpHeaderNames.CONTENT_TYPE))

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.assertResponseContent("[\"alpha\",{\"beta\":2}]")
  }

  @Test fun `should reject requests with abort`() {
    val channel = setupTestApplication(
      """
      from elide import Flask, abort

      app = Flask(__name__)

      @app.route("/")
      def forbidden():
          abort(403)

      app.bind()
      """.trimIndent(),
    )

    val response = channel.assertResponse(expectedStatus = HttpResponseStatus.FORBIDDEN)
    assertEquals("0", response.headers().get(HttpHeaderNames.CONTENT_LENGTH))

    channel.assertResponseContent("")
  }

  @Test fun `should serve static assets`() {
    val assetDir = syntheticRoot.resolve("static")
    assetDir.createDirectories()

    val asset = assetDir.resolve("hello.txt")
    asset.writeText("static-response")

    val channel = setupTestApplication(
      """
      from elide import Flask

      app = Flask(__name__)
      app.bind()
      """.trimIndent(),
    )

    val response = channel.assertResponse(
      request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/static/hello.txt"),
    )

    channel.runPendingTasks()
    executor.awaitContextTasks(channel)

    assertEquals("static-response".length, response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH))
    assertEquals("text/plain", response.headers().get(HttpHeaderNames.CONTENT_TYPE))

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.assertResponseContent("static-response")
  }

  @Test fun `should support routing`() {
    val channel = setupTestApplication(
      """
      from elide import Flask

      app = Flask(__name__)

      @app.route("/users/<int:userid>/posts/<path:slug>", methods=["GET"])
      def post_detail(userid, slug):
          return f"{userid}|{type(userid).__name__}|{slug}"

      @app.route("/users/<int:userid>", methods=["POST"])
      def update_user(userid):
          return "updated"

      app.bind()
      """.trimIndent(),
    )

    channel.assertResponse(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/users/42/posts/abc/def"))

    channel.runPendingTasks()
    executor.awaitContextTasks(channel)

    channel.assertResponseContent("42|int|abc/def")

    channel.runPendingTasks()

    val postRequest = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/users/42")
    channel.assertResponse(postRequest)

    channel.runPendingTasks()
    executor.awaitContextTasks(channel)

    channel.assertResponseContent("updated")
  }

  @Test fun `url_for should return handler path`() {
    val channel = setupTestApplication(
      """
      from elide import Flask, url_for

      app = Flask(__name__)

      @app.route("/items/<string:category>")
      def listing(category):
          return url_for("details", category=category, itemid=7, extra="foo")

      @app.route("/items/<string:category>/<int:itemid>")
      def details(category, itemid):
          return f"{category}:{itemid}"

      app.bind()
      """.trimIndent(),
    )

    channel.assertResponse(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/items/books"))

    channel.runPendingTasks()
    executor.awaitContextTasks(channel)

    channel.assertResponseContent("/items/books/7?extra=foo")
  }
}
