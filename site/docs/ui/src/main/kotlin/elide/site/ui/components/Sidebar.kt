package elide.site.ui.components

import csstype.Color
import csstype.None.none
import elide.site.ElideSite
import elide.site.ui.theme.Area
import elide.site.ui.theme.Sizes
import emotion.react.css
import mui.material.*
import mui.material.DrawerAnchor.left
import mui.material.DrawerVariant.permanent
import mui.system.Box
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.dom.html.ReactHTML.nav
import react.router.dom.NavLink
import react.router.useLocation

/** */
val Sidebar = FC<Props> {
  val path = useLocation().pathname

  Box {
    component = nav
    sx {
      gridArea = Area.Sidebar
      width = Sizes.Sidebar.Width
    }
    Drawer {
      variant = permanent
      anchor = left
      Box {
        sx {
          width = Sizes.Sidebar.Width
        }
        Toolbar()
        List {
          ElideSite.pages.forEach { page ->
            NavLink {
              to = page.path
              css {
                color = Color.currentcolor
                textDecoration = none
              }
              ListItemButton {
                selected = path == page.path
                ListItemText {
                  primary = ReactNode(page.label)
                }
              }
            }
          }
        }
      }
    }
  }
}
