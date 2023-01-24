package elide.site.ui.pages.legal

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.legal.mdx.LicenseMdx
import elide.site.ui.pages.legal.mdx.PrivacyMdx
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.section

/** Renders the privacy page for the Elide website. */
val Privacy = react.FC<react.Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Privacy"
      }
    }

    section {
      MDX.render(this, PrivacyMdx)
      br()
    }
  }
}

/** Renders the license page for the Elide website. */
val License = react.FC<react.Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"License"
      }
    }

    section {
      MDX.render(this, LicenseMdx)
      br()
    }
  }
}
