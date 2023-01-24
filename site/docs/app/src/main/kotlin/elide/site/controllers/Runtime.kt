package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Runtime
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Runtime guide page (top-level). */
@Page(name = "runtime") class Runtime : SitePageController(page = Runtime) {
  // Serve the runtime top page.
  @Get("/runtime") suspend fun top(request: HttpRequest<*>) = page(request)
}
