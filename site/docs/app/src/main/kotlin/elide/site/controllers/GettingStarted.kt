package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.GettingStarted
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Getting Started page (top-level). */
@Page(name = "getting-started") class GettingStarted : SitePageController(page = GettingStarted) {
  // Serve the getting-started top page.
  @Get("/getting-started") suspend fun top(request: HttpRequest<*>) = page(request)
}
