@file:Suppress(
  "WildcardImport",
  "MagicNumber",
)

package elide.benchmarks

import elide.server.annotations.Page
import elide.server.controller.PageController
import elide.server.css
import elide.server.html
import elide.server.script
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import kotlinx.coroutines.*
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.fontFamily
import kotlinx.html.body
import kotlinx.html.strong
import kotlinx.html.tagext.head
import kotlinx.html.title
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/** Tests for general [PageController] performance. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class PageBenchmarkDirect {
  lateinit var applicationContext: ApplicationContext
  lateinit var controller: SampleController

  @Serializable
  data class SomeResponse(
    val message: String,
  )

  @Page class SampleController : PageController() {
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
    fun json(): HttpResponse<String> {
      return HttpResponse.ok(
        Json.encodeToString(
          SomeResponse.serializer(),
          SomeResponse("Hello Elide!")
        )
      )
    }
  }

  @Setup
  fun setUp() {
    applicationContext = ApplicationContext.run()
    controller = applicationContext.getBean(SampleController::class.java)
  }

  /** Test: serve raw HTML. */
  @Benchmark fun controllerHTML(): HttpResponse<*> = runBlocking {
    controller.index()
  }

  /** Test: serve raw CSS. */
  @Benchmark fun controllerCSS(): HttpResponse<*> = runBlocking {
    controller.styles()
  }

  /** Test: serve JSON. */
  @Benchmark fun controllerServeJSON(): HttpResponse<String> = runBlocking {
    controller.json()
  }

  /** Test: serve an embedded asset. */
  @Benchmark fun controllerServeAsset(): HttpResponse<*> = runBlocking {
    controller.js(HttpRequest.GET("/scripts/ui.js"))
  }
}
