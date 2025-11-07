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
package elide.runtime.http.server.python.wsgi

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.gvm.python.PythonTest
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.HttpCleartextService
import elide.runtime.http.server.netty.NettyCallHandlerAdapter
import elide.runtime.http.server.netty.NettyHttpRequestBody
import elide.runtime.http.server.source
import elide.runtime.http.server.write

@MicronautTest(rebuildContext = true) class WsgiServerApplicationTest : PythonTest() {
  lateinit var executor: ContextAwareExecutor

  @BeforeEach fun setup() {
    executor = ContextAwareExecutor(
      maxContextPoolSize = 1,
      baseExecutor = Executors.newCachedThreadPool(),
      contextFactory = { engine.acquire().unwrap() },
    )
  }

  @AfterEach fun teardown() {
    executor.shutdown()
  }

  private fun ContextAwareExecutor.awaitContextTasks(channel: EmbeddedChannel) {
    val latch = submit { /* noop, just wait until the single context is available again */ }
    while (!latch.isDone) channel.runPendingTasks()
  }

  private fun ContextAwareExecutor.awaitContextTasks() {
    submit { /* noop, just wait until the single context is available again */ }.get()
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

  private fun testChannel(app: WsgiServerApplication): EmbeddedChannel {
    @Suppress("UNCHECKED_CAST")
    return EmbeddedChannel(
      NettyCallHandlerAdapter(
        application = app as HttpApplication<CallContext>,
        defaultServerName = "Elide/Test",
      ),
    )
  }

  private fun testWsgiApplication(
    @Language("Python") source: String,
    arguments: List<Any>? = null,
    binding: String = "app",
  ): WsgiServerApplication {
    val entrypoint = WsgiEntrypoint(
      source = Source.create("python", source),
      bindingName = binding,
      bindingArgs = arguments,
    )

    return WsgiServerApplication(entrypoint, executor)
  }

  @Test fun `incoming calls should be dispatched to wsgi callable`() {
    val app = testWsgiApplication(
      source = """
      def app(environ, send_response):
        send_response('200', (('x-test-header', 'test-value'), ('x-elide-test', 'true')))
        return [b'hello', b'world']
      """.trimIndent(),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.writeInbound(LastHttpContent.EMPTY_LAST_CONTENT)
    channel.runPendingTasks()

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected a response to be returned")
      assertEquals(HttpResponseStatus.OK, response.status(), "expected a 200 response")
    }

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.readOutbound<HttpContent>().let { content ->
      assertNotNull(content, "expected a content chunk")
      assertEquals("hello", content.content().toString(Charsets.UTF_8))
      content.release()
    }

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.readOutbound<HttpContent>().let { content ->
      assertNotNull(content, "expected a content chunk")
      assertEquals("world", content.content().toString(Charsets.UTF_8))
      content.release()
    }

    assertIs<LastHttpContent>(channel.readOutbound<HttpContent>())
  }

  @Test fun `should invoke app factory when arguments are provided`() {
    val app = testWsgiApplication(
      source = """
      def make_app(name):
        def app(environ, send_response):
          send_response('200', [('x-app-name', name)])
          return []
        
        return app
      """.trimIndent(),
      arguments = listOf("test-app"),
      binding = "make_app",
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    channel.writeInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"))
    channel.writeInbound(LastHttpContent.EMPTY_LAST_CONTENT)
    channel.runPendingTasks()

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected a response to be returned")
      assertEquals(HttpResponseStatus.OK, response.status(), "expected a 200 response")
      assertEquals("test-app", response.headers().get("x-app-name"))
    }

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    assertIs<LastHttpContent>(channel.readOutbound<HttpContent>())
  }

  @Test fun `should provide full wsgi environ`() {
    val assertions = mutableListOf<Pair<String, String>>()
    val app = testWsgiApplication(
      source = """
      def make_app(assert_eq):
        def app(environ, send_response):
          # CGI vars
          assert_eq(environ["REQUEST_METHOD"], "GET")
          assert_eq(environ["SCRIPT_NAME"], "Unnamed")
          assert_eq(environ["PATH_INFO"], "/test")
          assert_eq(environ["QUERY_STRING"], "q=1")
          assert_eq(environ["CONTENT_TYPE"], "text/plain")
          assert_eq(str(environ["CONTENT_LENGTH"]), "5")
          assert_eq(environ["SERVER_NAME"], "localhost")
          assert_eq(environ["SERVER_PORT"], "8080")
          assert_eq(environ["SERVER_PROTOCOL"], "HTTP/1.1")
          
          # WSGI environ
          assert_eq(str(environ["wsgi.version"]), "[1, 0]")
          assert_eq(environ["wsgi.url_scheme"], "http")
          assert_eq(str(environ["wsgi.multithread"]), "False")
          assert_eq(str(environ["wsgi.multiprocess"]), "False")
          assert_eq(str(environ["wsgi.run_once"]), "False")
          
          # headers
          assert_eq(environ["HTTP_X_API_KEY"], "abcd")
          
          # request body
          input = environ["wsgi.input"].read(5)
          assert_eq(input.decode("utf-8"), "hello")
          
          # response
          send_response('200', [])
          return []
        return app
      """.trimIndent(),
      binding = "make_app",
      arguments = listOf(ProxyExecutable { assertions.add(it[0].asString() to it[1].asString()) }),
    )

    val channel = testChannel(app)

    app.onStart(testStack())
    channel.runPendingTasks()

    val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test?q=1")
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, "5")
    request.headers().set("X-Api-Key", "abcd")

    channel.writeInbound(request)
    channel.writeInbound(DefaultLastHttpContent(Unpooled.copiedBuffer("hello", Charsets.UTF_8)))
    channel.runPendingTasks()

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    channel.readOutbound<HttpResponse>().let { response ->
      assertNotNull(response, "expected a response to be returned")
      assertEquals(HttpResponseStatus.OK, response.status(), "expected a 200 response")
    }

    executor.awaitContextTasks(channel)
    channel.runPendingTasks()

    assertIs<LastHttpContent>(channel.readOutbound<HttpContent>())

    assertions.forEach {
      assertEquals(it.first, it.second)
    }
  }

  @Test fun `should parse wsgi entrypoint spec`() {
    val entrypoint = WsgiEntrypoint.from(
      spec = "make_app(arg1,arg2)",
      source = Source.create("python", "stub"),
    )

    assertEquals("make_app", entrypoint.bindingName)
    assertContentEquals(listOf("arg1", "arg2"), entrypoint.bindingArgs)
  }

  @Test fun `error stream should emit logs`() {
    val counter = object : Logger by Logging.of(WsgiServerApplicationTest::class) {
      val messages = AtomicInteger()
      override fun error(msg: String?) {
        messages.incrementAndGet()
      }
    }

    val stream = WsgiErrorStream(counter)
    executor.execute {
      val context = Context.getCurrent()
      context.getBindings("python").putMember("err", stream)
      context.eval(
        /* languageId = */ "python",
        /* source = */
        """
        err.write('hello')
        err.flush()
        err.writelines(['world', '!'])
        err.flush()
        err.write("post")
        err.close()
        """.trimIndent(),
      )
    }
    executor.awaitContextTasks()

    assertEquals(3, counter.messages.get())
  }

  @Test fun `input stream should read blocking`() {
    val channel = EmbeddedChannel()
    val input = WsgiInputStream(executor, 11)

    val stream = NettyHttpRequestBody(channel.eventLoop())
    stream.source {
      it.write("hello")
      it.write("world\n")
      it.write("excess")
      it.end()
    }
    channel.runPendingTasks()

    stream.consume(input)
    channel.runPendingTasks()

    val messages = mutableListOf<String>()
    val emitMessage = ProxyExecutable { messages += it[0].asString().trim() }

    executor.execute {
      val context = Context.getCurrent()
      context.getBindings("python").apply {
        putMember("input", input)
        putMember("emit", emitMessage)
      }

      context.eval(
        /* languageId = */ "python",
        /* source = */
        """
        emit(input.read(5).decode('utf-8'))
        emit(input.readline(5).decode('utf-8'))
        emit(input.read(5).decode('utf-8'))
        """.trimIndent(),
      )
    }

    executor.awaitContextTasks(channel)

    assertContentEquals(listOf("hello", "world", ""), messages)
  }

  @Test fun `input stream should iterate over lines`() {
    val channel = EmbeddedChannel()
    val input = WsgiInputStream(executor, 12)

    val stream = NettyHttpRequestBody(channel.eventLoop())
    stream.source {
      it.write("hello\n")
      it.write("world\n")
      it.end()
    }
    channel.runPendingTasks()

    stream.consume(input)
    channel.runPendingTasks()

    val messages = mutableListOf<String>()
    val emitMessage = ProxyExecutable { messages += it[0].asString().trim() }

    executor.execute {
      val context = Context.getCurrent()
      context.getBindings("python").apply {
        putMember("input", input)
        putMember("emit", emitMessage)
      }

      context.eval(
        /* languageId = */ "python",
        /* source = */
        """
        for line in input:
          if line == b'':
            break
          emit(line.decode('utf-8'))
        """.trimIndent(),
      )
    }

    executor.awaitContextTasks(channel)

    assertContentEquals(listOf("hello", "world"), messages)
  }
}
