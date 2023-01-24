package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Tooling
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Legal pages (top-level). */
@Page(name = "legal") class Legal : SitePageController(page = Tooling) {
  // Serve the privacy page.
  @Get("/legal/privacy") suspend fun privacy(request: HttpRequest<*>) = page(request)

  // Serve the license page.
  @Get("/legal/license") suspend fun license(request: HttpRequest<*>) = page(request)
}
