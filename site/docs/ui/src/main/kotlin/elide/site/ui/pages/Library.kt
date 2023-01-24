package elide.site.ui.pages

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.pages.library.Packages as PackagesPage
import elide.site.ui.pages.library.Packages
import elide.site.ui.pages.library.mdx.LibraryMdx
import elide.site.ui.theme.Area
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.Box
import mui.system.sx
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.section
import react.*
import react.router.Outlet
import react.router.Route
import react.router.Routes

/** Library root page. */
private val LibraryRoot = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Elide as a Framework"
      }
    }

    section {
      MDX.render(this, LibraryMdx)
      br()
    }
  }
}

/** Elide-as-library root component and router. */
val Library = FC<Props> {
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
