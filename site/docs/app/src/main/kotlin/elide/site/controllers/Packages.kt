package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.library.Packages
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Library and packages page (top-level). */
@Page(name = "packages") class Packages : SitePageController(page = Packages) {
  // Serve the library top page.
  @Get("/library") suspend fun top(request: HttpRequest<*>) = page(request)

  // Serve the packages page.
  @Get("/library/packages") suspend fun pages(request: HttpRequest<*>) = page(request)
}
