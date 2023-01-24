package elide.site.ui.pages.tooling

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.tooling.mdx.BazelMdx
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.section
import react.*

/** Renders the Bazel tooling page on the Elide site. */
val Bazel = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Using Elide with Bazel"
      }
    }

    section {
      MDX.render(this, BazelMdx)
      br()
    }
  }
}
