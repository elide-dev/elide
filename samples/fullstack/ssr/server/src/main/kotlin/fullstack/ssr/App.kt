package fullstack.ssr

import elide.server.*
import elide.server.annotations.Page
import elide.server.controller.PageController
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.fontFamily
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import kotlinx.html.title

/** Self-contained application example, which serves an HTML page, with CSS, that says "Hello, Elide!". */
object App : Application {
  /** GET `/`: Controller for index page. */
  @Page class Index : PageController() {
    // Serve the page itself.
    @Get("/") suspend fun index(request: HttpRequest<*>) = ssr(request) {
      head {
        title { +"Hello, Elide!" }
        stylesheet("/styles/main.css")
      }
      body {
        streamSSR(this@Index, request)
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
    boot(args)
  }
}
