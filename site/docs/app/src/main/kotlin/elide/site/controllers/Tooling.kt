package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Tooling
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Tooling guide page (top-level). */
@Page(name = "tooling") class Tooling : SitePageController(page = Tooling) {
  // Serve the tooling top page.
  @Get("/tooling") suspend fun top(request: HttpRequest<*>) = page(request)
}
