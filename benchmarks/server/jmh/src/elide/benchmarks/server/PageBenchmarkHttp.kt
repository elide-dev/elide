/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:OptIn(
  DelicateCoroutinesApi::class,
  ExperimentalCoroutinesApi::class,
)
@file:Suppress(
  "WildcardImport",
  "MagicNumber",
)

package elide.benchmarks.server

import elide.server.annotations.Page
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
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.ReadTimeoutException
import io.micronaut.runtime.server.EmbeddedServer
import io.netty.channel.EventLoopGroup
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
  private val mainThreadSurrogate = newSingleThreadContext("server")
  private final val applicationContext: ApplicationContext = ApplicationContext.builder().propertySources(
    // disable SSL, auto-select port
    PropertySource.of(mapOf(
      "micronaut.server.ssl.enabled" to false,
      "micronaut.server.port" to -1,
      "grpc.server.enabled" to false,
    ))
  ).start()

  val embeddedServer: EmbeddedServer = applicationContext.getBean(EmbeddedServer::class.java)
  val mainEventLoopGroup: EventLoopGroup = applicationContext.getBean(EventLoopGroup::class.java)
  val readTimeouts: AtomicInteger = AtomicInteger(0)
  lateinit var client: BlockingHttpClient
  var port: AtomicInteger = AtomicInteger(-1)

  @Setup fun setUp() {
    Dispatchers.setMain(mainThreadSurrogate)
    embeddedServer.start()
    port.set(embeddedServer.port)
    client = HttpClient.create(
      URI.create("${embeddedServer.scheme}://${embeddedServer.host}:${port}").toURL()
    ).toBlocking()
  }

  @TearDown fun tearDown() {
    client.close()
    Dispatchers.resetMain()
    mainThreadSurrogate.close()
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
  private fun submitRequest(req: HttpRequest<Any>): HttpResponse<*> {
    return try {
      client.exchange<Any, Any>(req)
    } catch (rte: ReadTimeoutException) {
      val numberTotal = readTimeouts.incrementAndGet()
      println("Read timeout (count: $numberTotal)")
      HttpResponse.serverError("Read timeout")
    }
  }

  /** Test: serve a string. */
  @Benchmark fun controllerBasic() {
    submitRequest(HttpRequest
      .GET<Any>("/basic")
      .accept(MediaType.TEXT_PLAIN)
    )
  }

  /** Test: serve raw HTML. */
  @Benchmark fun controllerHTML() {
    submitRequest(HttpRequest.GET("/"))
  }

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
