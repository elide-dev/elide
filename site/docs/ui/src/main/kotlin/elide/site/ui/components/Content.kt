package elide.site.ui.components

import web.cssom.ClassName
import web.cssom.Overflow
import web.cssom.pct
import web.cssom.px
import elide.site.ElideSite
import elide.site.abstract.SitePage
import elide.site.ui.ElidePageProps
import elide.site.ui.pages.*
import elide.site.ui.pages.startup.GettingStarted
import elide.site.ui.pages.legal.Privacy
import elide.site.ui.pages.legal.License
import elide.site.ui.theme.Area
import mui.material.Typography
import mui.system.Box
import mui.system.sx
import react.ElementType
import react.create
import react.router.Outlet
import react.router.PathRoute
import react.router.Route
import react.router.Routes

private val defaultPadding = 30.px

/** Resolve a page component for the provided `name`. */
fun SitePage.component(): ElementType<*>? = when (this.name) {
  "runtime" -> Runtime
  "samples" -> Samples
  "library" -> Library
  "architecture" -> Architecture
  "getting-started" -> GettingStarted
  "tooling" -> Tooling
  "privacy" -> Privacy
  "license" -> License
  "not-found" -> NotFound
  else -> null
}

/** Main content zone/router/component host for the Elide site. */
val Content = react.FC<ElidePageProps> {
  val loadingFragment = react.Fragment.create {
    Typography {
      sx {
        padding = defaultPadding
      }
      +"Loading..."
    }
  }

  Box {
    className = ClassName("elide-site-content-container")

    sx {
      gridArea = Area.Content
      maxHeight = 100.pct
      maxWidth = 100.pct
      overflowY = Overflow.scroll
    }

    Routes {
      ElideSite.pages.filter { !it.hidden }.forEach { page ->
        PathRoute {
          key = page.name
          if (page.name == "home") index = true
          path = if (page.children.isEmpty()) {
            page.path
          } else {
            "${page.path}/*"
          }
          element = when (page.name) {
            "home" -> react.Fragment.create {
              Home {
                key = page.name
                full = true
              }
            }

            else -> react.Suspense.create {
              fallback = loadingFragment
              children = Box.create {
                component = page.component()
                sx {
                  gridArea = Area.Content
                  padding = defaultPadding
                }
                Outlet()
              }
            }
          }
        }
      }

      PathRoute {
        path = "/legal/privacy"
        element = react.Suspense.create {
          fallback = loadingFragment
          children = react.Fragment.create {
            Privacy {}
          }
        }
      }

      PathRoute {
        path = "/legal/license"
        element = react.Suspense.create {
          fallback = loadingFragment
          children = react.Fragment.create {
            License {}
          }
        }
      }

      PathRoute {
        path = "*"
        element = react.Fragment.create {
          NotFound {
            key = "not-found"
          }
        }
      }
    }
  }
}
