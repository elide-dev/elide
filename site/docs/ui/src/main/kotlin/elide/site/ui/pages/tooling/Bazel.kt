package elide.site.ui.pages.tooling

import csstype.ClassName
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.*

/** Renders the Bazel tooling page on the Elide site. */
val Bazel = FC<Props> {
  main {
    className = ClassName("elide-site-page center")

    div {
      +"Bazel"
    }
  }
}
