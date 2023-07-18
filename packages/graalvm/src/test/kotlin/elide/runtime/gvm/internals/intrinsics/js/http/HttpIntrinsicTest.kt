package elide.runtime.gvm.internals.intrinsics.js.http

import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.http.IncomingMessage
import elide.runtime.intrinsics.js.http.OutgoingMessage
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.graalvm.polyglot.Value
import java.net.InetAddress
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestCase internal class HttpIntrinsicTest : AbstractJsIntrinsicTest<HttpAPIInstrinsic>() {
  // Injected http intrinsic under test
  @Inject lateinit var http: HttpAPIInstrinsic
  
  override fun provide(): HttpAPIInstrinsic = http

  @Test override fun testInjectable() {
    assertNotNull(http, "should be able to inject http intrinsic instance")
  }
  
  @Test fun testGuestRequire() = executeGuest {
    // language=javascript
    """
    require("http");
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should get a return value for guest http module")
    assertTrue(it.returnValue()!!.isHostObject, "should get a host object as return value")
    assertIs<HttpAPIInstrinsic>(it.returnValue()!!.asHostObject(), "should get an injected intrinsic as return value")
  }

  @Test fun testCreateServer() = executeGuest {
    // language=javascript
    """
    const http = require("http");

    http.createServer({ }, (req, res) => {
      // handler
    })
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should get a return value for createServer function")
    assertTrue(it.returnValue()!!.isHostObject, "should get a host object as return value")
    assertIs<ServerIntrinsic>(it.returnValue()!!.asHostObject(), "should get an injected intrinsic as return value")
  }

  @Test fun testBindServerHost() = runTest {
    val valueReceived = CompletableDeferred<Unit>()
    val requestUrl = "http://localhost:8080/hello"

    // configure the server with a sample handler
    val server = ServerIntrinsic { req: IncomingMessage, res: OutgoingMessage ->
      assertEquals(
        expected = requestUrl,
        actual = req.url,
        message = "should receive correct url"
      )

      // write and send a simple value without specifying a callback or enc
      res.end(Value.asValue("Hello"), null, null)
    }

    // start the server
    server.listen(8080)

    // create a simple handler to send requests
    runTestHttpClient(
      port = 8080,
      sendRequest = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, requestUrl),
      expectResponseStatus = HttpResponseStatus.OK,
      expectResponseContent = "Hello",
      onComplete = { valueReceived.complete(Unit) }
    )

    valueReceived.await()
  }

  @Test fun testBindServerGuest() = executeGuest {
    // language=javascript
    """
    const http = require("http");

    http.createServer({/* options */}, (req, res) => {
      res.end("Hello")
    }).listen(8080)
    """
  }.thenAssert {
    val valueReceived = CompletableDeferred<Unit>()
    val requestUrl = "http://localhost:8080/hello"

    // create a simple handler to send requests
    runTestHttpClient(
      port = 8080,
      sendRequest = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, requestUrl),
      expectResponseStatus = HttpResponseStatus.OK,
      expectResponseContent = "Hello",
      onComplete = { valueReceived.complete(Unit) }
    )

    runTest { valueReceived.await() }
  }

  private fun runTestHttpClient(
    port: Int,
    sendRequest: HttpRequest,
    expectResponseStatus: HttpResponseStatus,
    expectResponseContent: String,
    onComplete: () -> Unit,
  ) {
    runTestHttpClient(
      port = port,
      onReady = { it.writeAndFlush(sendRequest) },
      onRead = { context, message ->
        // Verify the response sent by the server
        when(message) {
          // We received the response body, verify the content
          is HttpContent -> {
            assertEquals(
              expected = expectResponseContent,
              actual = message.content().toString(Charsets.UTF_8),
              message = "should get matching response content"
            )

            onComplete()
            context.close()
          }

          // We received the response headers, verify the status
          is HttpResponse -> {
            assertEquals(
              expected = expectResponseStatus,
              actual = message.status(),
              message = "should get response status 200 OK"
            )
          }

          // We didn't receive an HttpObject, this is an error
          else -> error("unexpected message type: $message")
        }
      }
    )
  }

  private fun runTestHttpClient(
    port: Int,
    onReady: (context: ChannelHandlerContext) -> Unit,
    onRead: (context: ChannelHandlerContext, message: Any) -> Unit,
  ) {
    // create a simple handler to send requests
    val handler = object : ChannelInboundHandlerAdapter() {
      override fun channelActive(ctx: ChannelHandlerContext) = onReady(ctx)
      override fun channelRead(ctx: ChannelHandlerContext, msg: Any) = onRead(ctx, msg)
    }

    // configure the client and connect
    Bootstrap().run {
      group(NioEventLoopGroup())
      channel(NioSocketChannel::class.java)

      handler(object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(channel: SocketChannel) {
          channel.pipeline().addLast(HttpClientCodec(), handler)
        }
      })

      connect(InetAddress.getLocalHost(), port).sync().channel()
    }
  }
}