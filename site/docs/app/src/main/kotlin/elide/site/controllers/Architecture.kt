package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Architecture
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Framework architecture page (top-level). */
@Page(name = "architecture") class Architecture : SitePageController(page = Architecture) {
  // Serve the root page.
  @Get("/architecture") suspend fun top(request: HttpRequest<*>) = page(request)
}
