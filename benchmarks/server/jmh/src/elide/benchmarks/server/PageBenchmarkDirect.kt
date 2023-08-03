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
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/** Tests for general [PageController] performance. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class PageBenchmarkDirect {
  private val mainThreadSurrogate = newSingleThreadContext("server")
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

  @Setup fun setUp() {
    Dispatchers.setMain(mainThreadSurrogate)
    applicationContext = ApplicationContext.run()
    controller = applicationContext.getBean(SampleController::class.java)
  }

  @TearDown fun tearDown() {
    applicationContext.close()
    Dispatchers.resetMain()
    mainThreadSurrogate.close()
  }

  /** Test: serve raw HTML. */
  @Benchmark fun controllerHTML(): HttpResponse<*> = runBlocking {
    controller.index()
  }

  /** Test: serve raw CSS. */
  @Benchmark fun controllerCSS(): HttpResponse<*> {
    return controller.styles()
  }

  /** Test: serve JSON. */
  @Benchmark fun controllerServeJSON(): HttpResponse<String> {
    return controller.json()
  }

  /** Test: serve an embedded asset. */
  @Benchmark fun controllerServeAsset(): HttpResponse<*> {
    return controller.styles()
  }
}
