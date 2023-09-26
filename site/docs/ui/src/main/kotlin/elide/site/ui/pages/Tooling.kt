package elide.site.ui.pages

import elide.site.ui.components.FullbleedPage
import elide.site.ui.pages.tooling.Bazel
import elide.site.ui.pages.tooling.Gradle
import elide.site.ui.pages.tooling.mdx.ToolingMdx
import elide.site.ui.theme.Area
import mui.system.Box
import mui.system.sx
import react.create
import react.router.Outlet
import react.router.PathRoute
import react.router.Routes

// Root page for the tooling section.
private val ToolingRoot = react.FC<react.Props> {
  FullbleedPage {
    heading = "Tooling Guides"
    component = ToolingMdx
  }
}

/** Renders the tooling page on the Elide site. */
val Tooling = react.FC<react.Props> {
  Routes {
    PathRoute {
      key = elide.site.pages.tooling.Gradle.name
      path = elide.site.pages.tooling.Gradle.path.removePrefix("/tools")
      element = react.Fragment.create {
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

    PathRoute {
      key = elide.site.pages.tooling.Bazel.name
      path = elide.site.pages.tooling.Bazel.path.removePrefix("/tools")
      element = react.Fragment.create {
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

    PathRoute {
      path = "*"
      index = true
      element = react.Fragment.create {
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
