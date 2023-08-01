package elide.server.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get
import kotlinx.css.*
import kotlinx.html.strong
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import kotlinx.html.title
import elide.server.*
import elide.server.annotations.Page
import elide.server.assets.AssetType

/** Sample page controller for testing. */
@Page class SamplePageController : PageController() {
  @Get("/") suspend fun indexPage() = html {
    head {
      title { +"Hello, Elide!" }
      stylesheet(asset("styles.base"), media = "screen", attrs = sortedMapOf("hello" to "cool"))
      stylesheet(asset("styles.base"))
      script(
        asset("scripts.ui"),
        async = true,
        defer = true,
        nomodule = true,
        attrs = sortedMapOf(
          "extra" to "attr",
        )
      )
      script(asset("scripts.ui"))
      stylesheet("/styles/base.css")
      script("/scripts/ui.js", defer = true)
    }
    body {
      strong {
        +"Hello Sample Page!"
      }
      script(asset("scripts.ui"))
      script("/scripts/ui.js", defer = true)
    }
  }

  @Get("/some-styles.css") fun styles() = css {
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

  @Get("/styles/base.css") suspend fun assetStyle(request: HttpRequest<Any>) = stylesheet(request) {
    module("styles.base")
    assetType(AssetType.STYLESHEET)
  }

  @Get("/styles/base.another.css") suspend fun assetGeneric(request: HttpRequest<Any>) = asset(request) {
    module("styles.base")
    assetType(AssetType.STYLESHEET)
  }

  @Get("/styles/base.other.css") suspend fun assetStyleExplicit(request: HttpRequest<Any>) =
    stylesheet(request, "styles.base")

  @Get("/scripts/ui.js") suspend fun assetScript(request: HttpRequest<Any>) = script(request) {
    module("scripts.ui")
  }

  @Get("/scripts/ui.other.js") suspend fun assetScriptExplicit(request: HttpRequest<Any>) =
    script(request, "scripts.ui")

  @Get("/something.txt") suspend fun somethingText() = staticFile(
    "something.txt",
    "text/plain",
  )

  @Get("/something-not-found.txt") suspend fun somethingMissing() = staticFile(
    "something-not-found.txt",
    "text/plain",
  )
}
