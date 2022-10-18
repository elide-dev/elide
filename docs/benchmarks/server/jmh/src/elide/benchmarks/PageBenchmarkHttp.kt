@file:Suppress(
  "WildcardImport",
  "MagicNumber",
)

package elide.benchmarks

import elide.server.annotations.Page
import elide.server.cfg.ServerConfigurator
import elide.server.controller.PageController
import elide.server.css
import elide.server.html
import elide.server.script
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.PropertySource
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.ReadTimeoutException
import io.micronaut.runtime.Micronaut
import io.micronaut.runtime.server.EmbeddedServer
import io.netty.channel.EventLoopGroup
import kotlinx.coroutines.*
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.fontFamily
import kotlinx.html.body
import kotlinx.html.strong
import kotlinx.html.tagext.head
import kotlinx.html.title
import org.openjdk.jmh.annotations.*
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Tests for general [PageController] performance. */
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class PageBenchmarkHttp {
  final val applicationContext: ApplicationContext = ApplicationContext.builder().propertySources(
    // disable SSL, auto-select port
    PropertySource.of(mapOf(
      "micronaut.server.ssl.enabled" to false,
      "micronaut.server.port" to -1,
      "micronaut.executors.default.n-threads" to 1,
      "micronaut.executors.io.n-threads" to 1,
      "micronaut.views.soy.enabled" to false,
      "micronaut.http.client.read-timeout" to 5,
      "micronaut.http.client.num-of-threads" to 1,
      "micronaut.http.client.pool.enabled" to true,
      "micronaut.server.thread-selection" to "MANUAL",
      "micronaut.http.client.pool.max-connections" to 25,
      "micronaut.netty.event-loops.default.num-threads" to 1,
      "micronaut.netty.event-loops.default.prefer-native-transport" to true,
      "grpc.server.enabled" to false,
    ))
  ).start()

  val embeddedServer: EmbeddedServer = applicationContext.getBean(EmbeddedServer::class.java)
  val mainEventLoopGroup: EventLoopGroup = applicationContext.getBean(EventLoopGroup::class.java)
  val readTimeouts: AtomicInteger = AtomicInteger(0)
  lateinit var client: HttpClient
  var port: AtomicInteger = AtomicInteger(8080)

  @Setup
  fun setUp() {
    embeddedServer.start()
    port.set(embeddedServer.port)
    client = HttpClient.create(
      URI.create("${embeddedServer.scheme}://${embeddedServer.host}:${port}").toURL()
    )
  }

  @TearDown
  fun tearDown() {
    client.close()
    embeddedServer.stop()
    applicationContext.close()
    mainEventLoopGroup.shutdownGracefully().get(10, TimeUnit.SECONDS)
  }

  data class SomeResponse(
    val message: String,
  )

  @Page class SampleController : PageController() {
    @Get("/basic", consumes = [
      MediaType.TEXT_PLAIN
    ], produces = [
      MediaType.TEXT_PLAIN
    ]) fun basic(): HttpResponse<String> = HttpResponse.ok(
      "Hello Benchmark"
    )

    @Get("/") suspend fun index() = html {
      head {
        title { +"Hello, Elide!" }
      }
      body {
        strong { +"Hello, Elide!" }
      }
    }

    // Serve styles for the page.
    @Get("/styles/main.css") fun styles() = css {
      rule("body") {
        backgroundColor = Color("#bada55")
      }
      rule("strong") {
        fontFamily = "-apple-system, BlinkMacSystemFont, sans-serif"
      }
    }

    // Serve the built & embedded JavaScript.
    @Get("/scripts/ui.js") suspend fun js(request: HttpRequest<Any>) = script(request) {
      module("scripts.ui")
    }

    // Serve the built & embedded JavaScript.
    @Get("/scripts/ui.json", produces = [MediaType.APPLICATION_JSON])
    fun json(): HttpResponse<SomeResponse> {
      return HttpResponse.ok(SomeResponse("Hello, Elide!"))
    }
  }

  @Suppress("SwallowedException")
  private fun submitRequest(req: HttpRequest<Any>): HttpResponse<Any> {
    return try {
      client.toBlocking().exchange(req)
    } catch (rte: ReadTimeoutException) {
      val numberTotal = readTimeouts.incrementAndGet()
      println("Read timeout (count: $numberTotal)")
      HttpResponse.serverError("Read timeout")
    }
  }

  /** Test: serve a string. */
  @Benchmark fun controllerBasic(): HttpResponse<*> = runBlocking {
    submitRequest(HttpRequest
      .GET<Any>("/basic")
      .accept(MediaType.TEXT_PLAIN)
    )
  }

  /** Test: serve raw HTML. */
//  @Benchmark fun controllerHTML(): HttpResponse<*> = runBlocking {
//    submitRequest(HttpRequest.GET("/"))
//  }

  /** Test: serve raw CSS. */
//  @Benchmark fun controllerCSS(): HttpResponse<*> = runBlocking {
//    submitRequest(HttpRequest.GET("/styles/main.css"))
//  }

  /** Test: serve JSON. */
//  @Benchmark fun controllerServeJSON(): HttpResponse<*> = runBlocking {
//    submitRequest(HttpRequest.GET("/scripts/ui.json"))
//  }

  /** Test: serve an embedded asset. */
//  @Benchmark fun controllerServeAsset(): HttpResponse<*> = runBlocking {
//    submitRequest(HttpRequest.GET("/scripts/ui.js"))
//  }
}
