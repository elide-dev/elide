package elide.site.ui.pages

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.tooling.Bazel
import elide.site.ui.pages.tooling.Gradle
import elide.site.ui.pages.tooling.mdx.ToolingMdx
import elide.site.ui.theme.Area
import mui.material.Typography
import mui.material.styles.TypographyVariant
import mui.system.Box
import mui.system.sx
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.section
import react.*
import react.router.Outlet
import react.router.Route
import react.router.Routes

// Root page for the tooling section.
private val ToolingRoot = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Tooling Guides"
      }
    }

    section {
      MDX.render(this, ToolingMdx)
      br()
    }
  }
}

/** Renders the tooling page on the Elide site. */
val Tooling = FC<Props> {
  Routes {
    Route {
      key = elide.site.pages.tooling.Gradle.name
      path = elide.site.pages.tooling.Gradle.path.removePrefix("/tools")
      element = Fragment.create {
        children = Box.create {
          component = Gradle
          sx {
            gridArea = Area.Content
            padding = defaultSubPagePadding
          }
          Outlet()
        }
      }
    }

    Route {
      key = elide.site.pages.tooling.Bazel.name
      path = elide.site.pages.tooling.Bazel.path.removePrefix("/tools")
      element = Fragment.create {
        children = Box.create {
          component = Bazel
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
          component = ToolingRoot
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
