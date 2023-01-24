package elide.site.ui.pages.library

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.library.mdx.PackagesMdx
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.section
import react.*

/** Renders the library packages page on the Elide site. */
val Packages = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Packages"
      }
    }

    section {
      MDX.render(this, PackagesMdx)
      br()
    }
  }
}
