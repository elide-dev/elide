package helloworld

import elide.server.controller.PageController
import elide.server.html
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.Micronaut.build
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.strong
import kotlinx.html.title


/** Self-contained application example, which serves an HTML page that says "Hello, Elide!". */
object App {
  /** GET `/`: Controller for index page. */
  @Controller class Index : PageController() {
    @Get("/") suspend fun index() = html {
      head {
        title { +"Hello, Elide!" }
      }
      body {
        strong { +"Hello, Elide!" }
      }
    }
  }

  /** Main entrypoint for the application. */
  @JvmStatic fun main(args: Array<String>) {
    build().args(*args).start()
  }
}
