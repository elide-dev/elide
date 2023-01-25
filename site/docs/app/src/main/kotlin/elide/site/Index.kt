package elide.site

import elide.server.*
import elide.server.annotations.Page
import elide.site.pages.Home
import elide.site.controllers.SitePageController
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Get
import kotlinx.css.*
import kotlinx.html.HEAD
import kotlinx.html.link
import java.util.*

/** GET `/`: Controller for index page. */
@Page(name = "index") class Index : SitePageController(page = Home) {
  // Make sure to pre-load homepage styles.
  override protected fun finalize(state: PageRenderState, response: MutableHttpResponse<ByteArray>) {
    super.finalize(state, response)

    if (response.status.code == 200) response.headers.apply {
      add(HttpHeaders.LINK, "</assets/home.min.css>; rel=preload; as=style")
      val ua = state.request.headers["sec-ch-ua"] ?: state.request.headers[HttpHeaders.USER_AGENT] ?: ""
      when {
        ua.contains("Google Chrome") || ua.contains("Chrome") || ua.contains("Chromium") -> {
          add(HttpHeaders.LINK, "<https://fonts.gstatic.com/l/font?kit=iJWZBXyIfDnIV5PNhY1KTN7Z-Yh-B4i1VU80V4fdkLdVHIPhLghxm6H4NTtG7lo4KH2dpQsfQ-dEPKg&skey=cee854e66788286d&v=v23>; rel=preload; as=font; crossorigin=anonymous; type=font/woff2")
        }
        ua.contains("Safari") -> {
          add(HttpHeaders.LINK, "<https://fonts.gstatic.com/l/font?kit=iJWZBXyIfDnIV5PNhY1KTN7Z-Yh-B4i1U080V4fdkLdVHIPhLghxm6H4NTtG7lo4KH2dpQsfQ-dEPKg&skey=cee854e66788286d&v=v23>; rel=preload; as=font; crossorigin=anonymous; type=font/woff")
        }
      }
    }
  }

  // Serve the root page.
  @Get("/") suspend fun indexPage(request: HttpRequest<*>) = page(request)

  // Serve doc styles.
  @Get("/assets/base.min.css") suspend fun baseStyles(request: HttpRequest<*>) = stylesheet(request) {
    module("styles.base")
  }.apply {
    finalizeAsset(request.locale.orElse(I18nPage.Defaults.locale), this)
  }

  // Serve home styles.
  @Get("/assets/home.min.css") suspend fun homeStyles(request: HttpRequest<*>) = stylesheet(request) {
    module("styles.home")
  }.apply {
    finalizeAsset(request.locale.orElse(I18nPage.Defaults.locale), this)
  }

  // Serve main docs script.
  @Get("/ui.js") suspend fun js(request: HttpRequest<*>) = script(request) {
    module("scripts.ui")
  }.apply {
    finalizeAsset(request.locale.orElse(I18nPage.Defaults.locale), this)
  }
}
