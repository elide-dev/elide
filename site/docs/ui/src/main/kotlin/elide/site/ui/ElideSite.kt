package elide.site.ui

import csstype.Auto.auto
import csstype.ClassName
import csstype.Display
import csstype.GridTemplateAreas
import csstype.array
import elide.site.ui.components.*
import elide.site.ui.theme.Area
import elide.site.ui.theme.Sizes
import mui.system.Box
import mui.system.sx
import react.FC
import react.Props


external interface ElideSiteProps: Props {
  var page: String?
  var mobile: Boolean
}

val ElideSite = FC<ElideSiteProps> {
    Box {
      className = ClassName("elide-site-container")

      sx {
        display = Display.grid
        gridTemplateRows = array(
          Sizes.Header.Height,
          auto,
        )
        gridTemplateColumns = array(
          Sizes.Sidebar.Width,
          auto,
        )
        gridTemplateAreas = GridTemplateAreas(
          arrayOf(Area.Header, Area.Header),
          if (it.mobile)
            arrayOf(Area.Content, Area.Content)
          else
            arrayOf(Area.Sidebar, Area.Content),
        )
      }

      Header()
      if (it.mobile) Menu() else Sidebar()
      Content()
    }
}
