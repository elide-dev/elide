package elide.site.ui.components

import csstype.Color
import csstype.None.none
import csstype.Position.Companion.absolute
import csstype.px
import elide.site.ElideSite
import elide.site.ui.theme.Sizes
import emotion.react.css
import mui.material.*
import mui.material.DrawerAnchor.left
import mui.system.Box
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.nav
import react.router.dom.NavLink
import react.router.useLocation
import mui.icons.material.Menu as MenuIcon

/** Render the main side-bar navigation menu for the Elide site. */
val Menu = FC<Props> {
  var isOpen by useState(false)
  val path = useLocation().pathname
  val lastPathname = path.substringAfterLast("/")

  Box {
    component = nav

    SwipeableDrawer {
      anchor = left
      open = isOpen
      onOpen = { isOpen = true }
      onClose = { isOpen = false }

      // TODO: Reorganize in mobile manner. No `List`
      Box {
        Toolbar()

        List {
          sx { width = Sizes.Sidebar.Width }

          ElideSite.pages.forEach { page ->
            NavLink {
              key = page.name
              to = page.path

              css {
                textDecoration = none
                color = Color.currentcolor
              }

              ListItemButton {
                selected = lastPathname == page.path || path == page.path

                ListItemText {
                  primary = ReactNode(page.label)
                }
              }
            }
          }
        }
      }
    }

    SpeedDial {
      sx {
        position = absolute
        bottom = 16.px
        left = 16.px
      }
      ariaLabel = "Menu"
      icon = MenuIcon.create()
      onClick = { isOpen = true }
    }
  }
}
