@file:Suppress("INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION")

package elide.site.ui.pages.startup

import csstype.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.startup.mdx.GettingStartedInstallLibrary
import elide.site.ui.pages.startup.mdx.GettingStartedInstallRuntime
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.aside
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.section
import react.*

/** Renders the Getting Started page on the Elide site. */
val GettingStarted = FC<Props> {
  main {
    className = ClassName("elide-site-page narrative")

    header {
      className = ClassName("elide-site-page__header")

      Typography {
        variant = TypographyVariant.h2
        +"Getting Started with Elide"
      }
    }

    aside {
      Typography {
        variant = TypographyVariant.h5
        +"Runtime or library"
      }

      p {
        +"Elide is both a runtime and a library. When used as a runtime, Elide is installed on your computer "
        +"and invoked as a command-line tool, just like Node or Python. When used as a library, Elide is installed as "
        +"a dependency in your project."
      }

      p {
        +"Right now, Elide supports use as a library from JVM apps only. As a runtime, Elide supports JavaScript. "
        +"In most cases, you'll want to install both."
      }

      br()
    }

    section {
      MDX.render(this, GettingStartedInstallRuntime)
      br()
    }

    section {
      MDX.render(this, GettingStartedInstallLibrary)
      br()
    }
  }
}
