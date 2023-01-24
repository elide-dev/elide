package elide.site.ui.pages

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.architecture.mdx.ArchitectureMdx
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.section
import react.*

/** Renders the top-level Architecture page on the Elide site. */
val Architecture = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Architecture"
      }
    }

    section {
      MDX.render(this, ArchitectureMdx)
      br()
    }
  }
}
