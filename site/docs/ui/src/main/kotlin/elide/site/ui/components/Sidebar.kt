package elide.site.ui.components

import web.cssom.ClassName
import web.cssom.Color
import web.cssom.Display
import web.cssom.None.Companion.none
import elide.site.ElideSite
import elide.site.abstract.PageInfo
import elide.site.pages.Home
import elide.site.ui.theme.Area
import elide.site.ui.theme.Sizes
import emotion.react.css
import mui.icons.material.ExpandMore
import mui.material.Collapse
import mui.material.Drawer
import mui.material.List
import mui.material.ListItemButton
import mui.material.ListItemText
import mui.material.Toolbar
import mui.material.DrawerAnchor.Companion.left
import mui.material.DrawerVariant.Companion.permanent
import mui.system.Box
import mui.system.sx
import react.dom.html.ReactHTML.nav
import react.router.dom.NavLink
import react.router.useLocation

/** Sidebar navigation entry properties. */
external interface SidebarNavEntryProps : react.Props {
  /** Current location. */
  var path: String

  /** Assigned page. */
  var page: PageInfo

  /** Whether this entry has children. */
  var hasChildren: Boolean

  /** Whether this entry is a child entry. */
  var isChild: Boolean

  /** Parent page info, if this entry is a child entry. */
  var parent: PageInfo?
}

/** Render a single navigation entry, potentially with children. */
val NavEntry = react.FC<SidebarNavEntryProps> {
  ListItemButton {
    selected = it.path == it.page.path

    css {
      color = Color.currentcolor
      textDecoration = none
      display = Display.flex
    }

    ListItemText {
      primary = react.ReactNode(it.page.label)
      className = if (it.isChild) {
        ClassName("elide-nav-subnav__text")
      } else {
        ClassName("elide-nav__text")
      }
    }

    if (it.hasChildren) {
      ExpandMore {}
    }
  }
}

/** Group of related sub-navigation entries. */
private val SubnavGroup = react.FC<SidebarNavEntryProps> {
  NavLink {
    key = it.page.name
    to = it.page.path

    css {
      color = Color.currentcolor
      textDecoration = none
    }

    NavEntry {
      this.hasChildren = true
      this.isChild = false
      this.path = it.path
      this.page = it.page
    }
  }

  // <Collapse in={open} timeout="auto" unmountOnExit>
  Collapse {
    timeout = "auto"
    `in` = true

    List {
      it.page.children.forEach { subpage ->
        NavLink {
          key = subpage.name
          to = subpage.path

          css {
            color = Color.currentcolor
            textDecoration = none
          }

          NavEntry {
            this.hasChildren = false
            this.isChild = true
            this.path = it.path
            this.page = subpage
            this.isChild = true
            this.parent = it.page
          }
        }
      }
    }
  }
}

/** Render the navigation sidebar. */
val Sidebar = react.FC<react.Props> {
  val path = useLocation().pathname

  Box {
    component = nav
    className = ClassName("elide-sidebar")

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
          ElideSite.pages.filter { it.name != Home.name && !it.hidden }.forEach { page ->
            val hasChildren = page.children.isNotEmpty()

            if (hasChildren) {
              SubnavGroup {
                this.hasChildren = true
                this.path = path
                this.page = page
              }
            } else {
              NavLink {
                to = page.path

                css {
                  color = Color.currentcolor
                  textDecoration = none
                }

                NavEntry {
                  this.hasChildren = false
                  this.path = path
                  this.page = page
                }
              }
            }
          }
        }
      }
    }
  }
}
