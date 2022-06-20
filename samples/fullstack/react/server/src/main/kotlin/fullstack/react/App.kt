package fullstack.react

import elide.server.*
import io.micronaut.http.MediaType
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
  @Controller class Index {
    // Serve the page itself.
    @Get("/") fun index() = html {
      head {
        title { +"Hello, Elide!" }
        stylesheet(this@Index::styles)
        script(this@Index::js, defer = true)
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
    @Get("/scripts/ui.js") fun js() = asset(
      "frontend.js",
      "js",
      MediaType("application/javascript", "js"),
    )
  }

  /** Main entrypoint for the application. */
  @JvmStatic fun main(args: Array<String>) {
    build().args(*args).start()
  }
}
