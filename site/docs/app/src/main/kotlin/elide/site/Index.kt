package elide.site

import elide.server.*
import elide.server.annotations.Page
import elide.site.pages.Home
import elide.site.controllers.SitePageController
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get
import kotlinx.css.*

/** GET `/`: Controller for index page. */
@Page(name = "index") class Index : SitePageController(page = Home) {
  // Serve the root page.
  @Get("/") suspend fun indexPage(request: HttpRequest<*>) = page(request)

  // Serve doc styles.
  @Get("/assets/base.css") suspend fun baseStyles(request: HttpRequest<*>) = stylesheet(request) {
    module("styles.base")
  }

  // Serve main docs script.
  @Get("/ui.js") suspend fun js(request: HttpRequest<*>) = script(request) {
    module("scripts.ui")
  }
}
