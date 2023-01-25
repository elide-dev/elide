package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.library.Packages
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Library and packages page (top-level). */
@Page(name = "packages") class Packages : SitePageController(page = Packages) {
  // Add FOSSA to `img-src` in CSP.
  protected override fun csp(state: PageRenderState): List<Pair<String, String>> = super.csp(state).map { stanza ->
    val (directive, policy) = stanza
    if (directive == "img-src") {
      directive to "$policy https://app.fossa.com https://codecov.io https://img.shields.io"
    } else {
      stanza
    }
  }

  // Serve the library top page.
  @Get("/library") suspend fun top(request: HttpRequest<*>) = page(request)

  // Serve the packages page.
  @Get("/library/packages") suspend fun pages(request: HttpRequest<*>) = page(request)
}
