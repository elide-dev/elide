package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Packages
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Library packages page (top-level). */
@Page(name = "packages") class Packages : SitePageController(page = Packages) {
  // Serve the packages top page.
  @Get("/packages") suspend fun top(request: HttpRequest<*>) = page(request)
}
