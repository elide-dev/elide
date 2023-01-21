package elide.site

import elide.server.*
import elide.server.annotations.Page
import elide.site.pages.Home
import elide.site.controllers.SitePageController
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get
import kotlinx.css.*
import kotlinx.html.HEAD
import kotlinx.html.link

/** GET `/`: Controller for index page. */
@Page(name = "index") class Index : SitePageController(page = Home) {
  // add masthead font to set of page fonts
  override fun fonts(): List<String> = super.fonts().plus(listOf(
    "https://fonts.googleapis.com/css2?family=Rubik&display=block&text=.'EIabcdefghiklmnoprstuwxy",
  ))

  // Serve the root page.
  @Get("/") suspend fun indexPage(request: HttpRequest<*>) = page(request)

  // Serve doc styles.
  @Get("/assets/base.min.css") suspend fun baseStyles(request: HttpRequest<*>) = stylesheet(request) {
    module("styles.base")
  }

  // Serve home styles.
  @Get("/assets/home.min.css") suspend fun homeStyles(request: HttpRequest<*>) = stylesheet(request) {
    module("styles.home")
  }

  // Serve main docs script.
  @Get("/ui.js") suspend fun js(request: HttpRequest<*>) = script(request) {
    module("scripts.ui")
  }
}
