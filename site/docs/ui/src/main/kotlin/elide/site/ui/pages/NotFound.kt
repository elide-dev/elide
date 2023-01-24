package elide.site.ui.pages

import csstype.ClassName
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.*

/** Renders a not-found page. */
val NotFound = FC<Props> {
  main {
    className = ClassName("elide-site-page errorpage")

    div {
      className = ClassName("elide-site-error")

      Typography {
        variant = TypographyVariant.h2
        +"Woops!"
      }
      br()

      Typography {
        variant = TypographyVariant.body1

        +"That page couldn't be located."
      }
    }
  }
}
