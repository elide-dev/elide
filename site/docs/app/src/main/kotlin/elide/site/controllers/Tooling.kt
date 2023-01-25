package elide.site.controllers

import elide.server.annotations.Page
import elide.site.pages.Tooling
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Get

/** Tooling guide page (top-level). */
@Page(name = "tooling") class Tooling : SitePageController(page = Tooling) {
  // Serve the tooling top page.
  @Get("/tools") suspend fun top(request: HttpRequest<*>) = page(request)

  // Serve the Gradle tooling page.
  @Get("/tools/gradle") suspend fun gradle(request: HttpRequest<*>) = page(request)

  // Serve the Bazel tooling page.
  @Get("/tools/bazel") suspend fun bazel(request: HttpRequest<*>) = page(request)
}
