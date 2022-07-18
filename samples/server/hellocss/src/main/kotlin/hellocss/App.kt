package hellocss

import elide.server.controller.PageController
import elide.server.css
import elide.server.html
import elide.server.stylesheet
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.Micronaut.build
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.fontFamily
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.strong
import kotlinx.html.title

/** Self-contained application example, which serves an HTML page, with CSS, that says "Hello, Elide!". */
object App {
  /** GET `/`: Controller for index page. */
  @Controller class Index : PageController() {
    // Serve the page itself.
    @Get("/") suspend fun index() = html {
      head {
        title { +"Hello, Elide!" }
        stylesheet("/styles/main.css")
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
  }

  /** Main entrypoint for the application. */
  @JvmStatic fun main(args: Array<String>) {
    build().args(*args).start()
  }
}
