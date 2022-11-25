package elide.docs

import elide.server.*
import elide.server.annotations.Page
import elide.server.controller.PageController
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get
import kotlinx.css.*
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import kotlinx.html.title

/** GET `/`: Controller for index page. */
@Page(name = "index") class Index : PageController() {
  // Serve the root page.
  @Get("/") suspend fun indexPage(request: HttpRequest<*>) = ssr(request) {
    head {
      title { +"Elide v3 | Rapid development framework for Kotlin" }
      stylesheet("/styles/base.css")
      stylesheet("/styles/main.css")
      script("/scripts/ui.js", defer = true)
    }
    body {
      injectSSR(this@Index, request)
    }
  }

  // Serve doc styles.
  @Get("/styles/base.css") suspend fun baseStyles(request: HttpRequest<*>) = stylesheet(request) {
    module("styles.base")
  }

  // Customizations for home page.
  @Get("/styles/main.css") fun styles() = css {
    rule("main") {
      backgroundColor = Color.white
      maxWidth = 600.px
      maxHeight = 800.px
    }
    rule(".docs-app-container") {
      display = Display.flex
      justifyContent = JustifyContent.center
      alignItems = Align.center
    }
  }

  // Serve main docs script.
  @Get("/scripts/ui.js") suspend fun js(request: HttpRequest<*>) = script(request) {
    module("scripts.ui")
  }
}
