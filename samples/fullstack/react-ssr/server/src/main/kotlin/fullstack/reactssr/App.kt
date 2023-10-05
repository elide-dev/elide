@file:Suppress("WildcardImport")

package fullstack.reactssr

import elide.server.*
import elide.server.annotations.Page
import elide.server.controller.PageWithProps
import elide.ssr.type.RequestState
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import kotlinx.css.*
import kotlinx.html.title
import elide.ssr.annotations.Props
import elide.vm.annotations.Polyglot

/** Self-contained application example, which serves an HTML page, with CSS, that says "Hello, Elide!". */
object App : Application {
  /** State properties for the root page. */
  @Props data class HelloProps (
    @Polyglot val name: String = "Elide"
  )

  /** GET `/`: Controller for index page. */
  @Page class Index : PageWithProps<HelloProps>(HelloProps::class) {
    /** @return Props to use when rendering this page. */
    override suspend fun props(state: RequestState) =
      HelloProps(name = state.request.parameters["name"] ?: "Elide v3")

    // Serve the root page.
    @Get("/") suspend fun index(request: HttpRequest<*>) = html(request) {
      head {
        title { +"Hello, Elide!" }
        stylesheet("/styles/base.css")
        stylesheet("/styles/main.css")
        script("/scripts/ui.js", defer = true)
      }
      body {
        render()
      }
    }

    // Serve the page itself.
    @Get("/basic", produces = [MediaType.TEXT_PLAIN])
    fun basic() = HttpResponse.ok("Hello Elide!")

    // Serve an embedded asset.
    @Get("/styles/base.css") suspend fun baseStyles(request: HttpRequest<*>) = stylesheet(request) {
      module("styles.base")
    }

    // Serve styles for the page.
    @Get("/styles/main.css") fun styles() = css {
      rule("main") {
        backgroundColor = Color.white
        maxWidth = 600.px
        maxHeight = 800.px
      }
      rule(".sample-app-container") {
        display = Display.flex
        justifyContent = JustifyContent.center
        alignItems = Align.center
      }
    }

    // Serve the built & embedded JavaScript (for browser execution).
    @Get("/scripts/ui.js") suspend fun js(request: HttpRequest<*>) = script(request) {
      module("scripts.ui")
    }
  }

  /** Main entrypoint for the application. */
  @JvmStatic fun main(args: Array<String>) {
    boot(args)
  }
}
