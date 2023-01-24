package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Tooling
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Legal pages (top-level). */
@Page(name = "legal") class Legal : SitePageController(page = Tooling) {
  // Add FOSSA to `img-src` in CSP.
  protected override fun csp(): List<Pair<String, String>> = super().csp().map { stanza ->
    val (directive, policy) = stanza
    if (directive == "img-src") {
      "img-src" to "$policy https://app.fossa.com https://img.shields.io"
    } else {
      stanza
    }
  }

  // Serve the privacy page.
  @Get("/legal/privacy") suspend fun privacy(request: HttpRequest<*>) = page(request)

  // Serve the license page.
  @Get("/legal/license") suspend fun license(request: HttpRequest<*>) = page(request)
}
