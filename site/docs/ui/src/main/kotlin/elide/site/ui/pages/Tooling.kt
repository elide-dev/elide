package elide.site.ui.pages

import csstype.ClassName
import elide.site.ui.pages.tooling.Bazel
import elide.site.ui.pages.tooling.Gradle
import elide.site.ui.theme.Area
import mui.system.Box
import mui.system.sx
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.*
import react.router.Outlet
import react.router.Route
import react.router.Routes

// Root page for the tooling section.
private val ToolingRoot = FC<Props> {
  div {
    +"Tooling root"
  }
}

/** Renders the tooling page on the Elide site. */
val Tooling = FC<Props> {
  main {
    className = ClassName("elide-site-page center")

    div {
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
  }
}
