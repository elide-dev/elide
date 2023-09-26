package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Samples
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Code samples page (top-level). */
@Page(name = "samples") open class Samples : SitePageController(page = Samples) {
  // Serve the samples top page.
  @Get("/samples") suspend fun top(request: HttpRequest<*>) = page(request)
}
