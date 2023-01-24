package elide.site.ui.pages

import elide.site.ui.components.FullbleedPage
import elide.site.pages.library.Packages as PackagesPage
import elide.site.ui.pages.library.Packages
import elide.site.ui.pages.library.mdx.LibraryMdx
import elide.site.ui.theme.Area
import mui.system.Box
import mui.system.sx
import react.create
import react.router.Outlet
import react.router.Route
import react.router.Routes

/** Library root page. */
private val LibraryRoot = react.FC<react.Props> {
  FullbleedPage {
    heading = "Elide as a Framework"
    component = LibraryMdx
  }
}

/** Elide-as-library root component and router. */
val Library = react.FC<react.Props> {
  Routes {
    Route {
      key = PackagesPage.name
      path = PackagesPage.path.removePrefix("/library")
      element = react.Fragment.create {
        children = Box.create {
          component = Packages
          sx {
            gridArea = Area.Content
            padding = defaultSubPagePadding
          }
          Outlet()
        }
      }
    }

    Route {
      path = "*"
      index = true
      element = react.Fragment.create {
        children = Box.create {
          component = LibraryRoot
          sx {
            gridArea = Area.Content
            padding = defaultSubPagePadding
          }
          Outlet()
        }
      }
    }
  }
}
