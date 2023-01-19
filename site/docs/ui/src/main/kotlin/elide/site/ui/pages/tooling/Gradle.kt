package elide.site.ui.pages.tooling

import csstype.ClassName
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.*

/** Renders the Gradle tooling page on the Elide site. */
val Gradle = FC<Props> {
  main {
    className = ClassName("elide-site-page center")

    div {
      +"Gradle"
    }
  }
}
