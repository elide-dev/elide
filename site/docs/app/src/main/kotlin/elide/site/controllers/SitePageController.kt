package elide.site.controllers

import elide.server.*
import elide.server.controller.PageController
import elide.server.controller.PageWithProps
import elide.site.AppServerProps
import elide.site.Assets
import elide.site.ElideSite
import elide.site.abstract.SitePage
import io.micronaut.http.HttpRequest
import jakarta.inject.Inject
import kotlinx.html.BODY
import kotlinx.html.HEAD
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import kotlinx.html.link
import kotlinx.html.title

/** Extend a [PageController] with access to a [SitePage] configuration. */
abstract class SitePageController protected constructor(val page: SitePage) : PageWithProps<AppServerProps>(
  AppServerProps.serializer(),
  AppServerProps(page = page.name),
) {
  companion object {
    const val enableSSR = true
    const val enableStreaming = true
  }

  // Site-level info resolved for the current locale.
  @Inject internal lateinit var siteInfo: ElideSite.SiteInfo

  /** @return Rendered page title. */
  protected fun renderTitle(): String {
    return if (page.title == "Elide") {
      siteInfo.title
    } else {
      "${page.title} | ${siteInfo.title}"
    }
  }

  /**
   *
   */
  protected open fun pageHead(): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "icon"
      href = "/images/favicon.png"
      type = "image/png"
    }
    link {
      rel = "icon"
      href = "/images/favicon.svg"
      sizes = "any"
      type = "image/svg+xml"
    }
  }

  /**
   *
   */
  protected open fun pageBody(): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    if (enableSSR) {
      if (enableStreaming) streamSSR(this@SitePageController, it)
      else injectSSR(this@SitePageController, it)
    }
  }

  /**
   *
   */
  protected open suspend fun page(
    request: HttpRequest<*>,
    head: suspend HEAD.(HttpRequest<*>) -> Unit,
    block: suspend BODY.(HttpRequest<*>) -> Unit,
  ) = ssr(request) {
    head {
      title { +renderTitle() }
      stylesheet(Assets.Styles.base)
      script(Assets.Scripts.ui, defer = true)
      head.invoke(this@head, request)
    }
    body {
      block.invoke(this@body, request)
    }
  }

  /**
   *
   */
  protected open suspend fun page(
    request: HttpRequest<*>,
    block: suspend BODY.(HttpRequest<*>) -> Unit = pageBody(),
  ) = page(
    request,
    pageHead(),
    block,
  )
}
