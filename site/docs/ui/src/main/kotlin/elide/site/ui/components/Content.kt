package elide.site.ui.components

import csstype.px
import elide.site.ElideSite
import elide.site.abstract.SitePage
import elide.site.ui.pages.*
import elide.site.ui.theme.Area
import mui.material.Typography
import mui.system.Box
import mui.system.sx
import react.*
import react.router.Outlet
import react.router.Route
import react.router.Routes

private val DEFAULT_PADDING = 30.px

/** Resolve a page component for the provided [name]. */
fun SitePage.component(): ElementType<*>? = when (this.name) {
  "home" -> Home
  "architecture" -> Architecture
  "getting-started" -> GettingStarted
  "tooling" -> Tooling
  else -> null
}

/** Main content zone/router/component host for the Elide site. */
val Content = FC<Props> {
  Routes {
    ElideSite.pages.forEach { page ->
      Route {
        key = page.name
        if (page.name == "home") index = true
        path = page.path
        element = Box.create {
          component = page.component()
          sx {
            gridArea = Area.Content
            padding = DEFAULT_PADDING
          }
          Outlet()
        }
      }
    }

    Route {
      path = "*"
      element = Typography.create { +"Not Found" }
    }
  }
}
