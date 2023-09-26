@file:Suppress("INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION")

package elide.site.ui.pages.startup

import web.cssom.ClassName
import elide.site.ui.MDX
import elide.site.ui.pages.startup.mdx.GettingStartedInstallLibrary
import elide.site.ui.pages.startup.mdx.GettingStartedInstallRuntime
import mui.material.Link
import mui.material.Typography
import mui.material.styles.TypographyVariant
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.aside
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.section
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.strong

/** Renders the Getting Started page on the Elide site. */
val GettingStarted = react.FC<react.Props> {
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
      br()

      Typography {
        variant = TypographyVariant.body1

        +"Elide is both a "

        strong {
          +"library"
        }

        +" and a "

        strong {
          +"runtime"
        }

        +". When used as a runtime, Elide is installed on your computer, or on your server, "
        +"and invoked as a command-line tool, just like Node or Python. When used as a library, Elide is installed as "
        +"a dependency in your project."
      }
      br()

      Typography {
        variant = TypographyVariant.body1

        +"Right now, Elide supports use as a library from JVM apps only. As a runtime, Elide supports JavaScript. "
        +"In most cases, you'll want to install both."
      }

      br()
      Typography {
        variant = TypographyVariant.h6
        +"Which is best for my use case?"
      }
      br()

      Typography {
        variant = TypographyVariant.body1

        +"The answer largely depends how you intend to use Elide. Library use is only available for JVM languages at "
        +"this time. For more information to help you decide, consult the "

        Link {
          component = a
          href = "/architecture"

          +"Architecture"
        }

        +" guide."
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

    section {
      Typography {
        variant = TypographyVariant.h4
        +"Next steps"
      }
      br()

      Typography {
        variant = TypographyVariant.body1

        +"Now that you've installed Elide, you can start writing code! Check out the "

        Link {
          component = a
          href = "/samples"
          +"code samples"
        }

        +" and "

        Link {
          component = a
          href = "/tools"
          +"tooling guides"
        }

        +" for your favorite use case."
      }
      br()
      br()
    }
  }
}
