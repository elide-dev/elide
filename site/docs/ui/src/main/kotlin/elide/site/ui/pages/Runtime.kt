package elide.site.ui.pages

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.runtime.mdx.RuntimeMdx
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.section
import react.*

/** Renders the Runtime page on the Elide website. */
val Runtime = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Elide as a Runtime"
      }
    }

    section {
      MDX.render(this, RuntimeMdx)
      br()
    }
  }
}
