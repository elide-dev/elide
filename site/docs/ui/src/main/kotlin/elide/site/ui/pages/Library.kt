package elide.site.ui.pages

import csstype.ClassName
import csstype.px
import elide.site.pages.library.Packages as PackagesPage
import elide.site.ui.pages.library.Packages
import elide.site.ui.theme.Area
import mui.system.Box
import mui.system.sx
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.*
import react.router.Outlet
import react.router.Route
import react.router.Routes

/** Library root page. */
private val LibraryRoot = FC<Props> {
  div {
    +"Library root"
  }
}

/** Elide-as-library root component and router. */
val Library = FC<Props> {
  main {
    className = ClassName("elide-site-page center")

    div {
      Routes {
        Route {
          key = PackagesPage.name
          path = PackagesPage.path.removePrefix("/library")
          element = Fragment.create {
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
          element = Fragment.create {
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
  }
}
