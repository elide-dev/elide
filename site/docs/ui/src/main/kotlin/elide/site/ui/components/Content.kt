package elide.site.ui.components

import csstype.Overflow
import csstype.pct
import csstype.px
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
import react.*
import react.router.Outlet
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
val Content = FC<ElidePageProps> {
  val loadingFragment = Fragment.create {
    Typography {
      sx {
        padding = defaultPadding
      }
      +"Loading..."
    }
  }

  Box {
    sx {
      gridArea = Area.Content
      overflowX = Overflow.scroll
      maxHeight = 100.pct
    }
    Routes {
      ElideSite.pages.filter { !it.hidden }.forEach { page ->
        Route {
          key = page.name
          if (page.name == "home") index = true
          path = if (page.children.isEmpty()) {
            page.path
          } else {
            "${page.path}/*"
          }
          element = when (page.name) {
            "home" -> Fragment.create {
              Home {
                key = page.name
                full = true
              }
            }

            else -> Suspense.create {
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

      Route {
        path = "/legal/privacy"
        element = Suspense.create {
          fallback = loadingFragment
          children = Fragment.create {
            Privacy {}
          }
        }
      }

      Route {
        path = "/legal/license"
        element = Suspense.create {
          fallback = loadingFragment
          children = Fragment.create {
            License {}
          }
        }
      }

      Route {
        path = "*"
        element = Fragment.create {
          NotFound {
            key = "not-found"
          }
        }
      }
    }
  }
}
