package elide.site.ui.components

import web.cssom.Color
import web.cssom.None.Companion.none
import web.cssom.Position.Companion.absolute
import web.cssom.px
import elide.site.ElideSite
import elide.site.ui.theme.Sizes
import emotion.react.css
import mui.material.List
import mui.material.ListItemButton
import mui.material.ListItemText
import mui.material.SwipeableDrawer
import mui.material.SpeedDial
import mui.material.Toolbar
import mui.material.DrawerAnchor.Companion.left
import mui.system.Box
import mui.system.sx
import react.create
import react.dom.html.ReactHTML.nav
import react.router.dom.NavLink
import react.router.useLocation
import mui.icons.material.Menu as MenuIcon

/** Render the main side-bar navigation menu for the Elide site. */
val Menu = react.FC<react.Props> {
  var isOpen by react.useState(false)
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

          ElideSite.pages.filter {
            it.name != "home" && !it.hidden
          }.forEach { page ->
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
                  primary = react.ReactNode(page.label)
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
