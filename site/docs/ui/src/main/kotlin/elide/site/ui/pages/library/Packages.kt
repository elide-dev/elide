package elide.site.ui.pages.library

import csstype.ClassName
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.*

/** Renders the library packages page on the Elide site. */
val Packages = FC<Props> {
  main {
    className = ClassName("elide-site-page center")

    div {
      +"Packages"
    }
  }
}
