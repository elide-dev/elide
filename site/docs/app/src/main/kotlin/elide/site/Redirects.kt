package elide.site

import elide.server.*
import elide.server.annotations.Page
import elide.server.controller.PageController
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import kotlinx.css.*
import java.net.URI

/** Redirect / link controller. */
@Page(name = "links", precompile = false) class Redirects : PageController() {
  // Redirect to the project GitHub page.
  @Get("/github") fun goToGithub(): HttpResponse<*> = HttpResponse.temporaryRedirect<Any>(
    URI.create(ExternalLinks.github)
  )

  // Redirect to the Kotlin reference docs.
  @Get("/kotlin") fun kotlinDocs(): HttpResponse<*> = HttpResponse.temporaryRedirect<Any>(
    URI.create(SiteLinks.ReferenceDocs.kotlin)
  )

  // Redirect to the Java reference docs.
  @Get("/javadoc") fun javaDocs(): HttpResponse<*> = HttpResponse.temporaryRedirect<Any>(
    URI.create(SiteLinks.ReferenceDocs.javadoc)
  )
}
