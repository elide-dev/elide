@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package elide.site.ui.pages

import csstype.*
import elide.site.Assets
import elide.site.abstract.SitePage
import elide.site.pages.GettingStarted
import elide.site.pages.Samples
import elide.site.pages.Tooling
import elide.site.ui.ElidePageProps
import elide.site.ui.components.CodeSample
import elide.site.ui.components.SyntaxLanguage
import elide.site.ui.components.ThemeContext
import elide.site.ui.components.ThemePackage
import js.core.jso
import lib.reactSyntaxHighlighter.SyntaxThemeTomorrowNight
import mui.icons.material.GitHub
import mui.icons.material.MenuBook
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.sx
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.footer
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.header
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.section
import react.dom.html.ReactHTML.span
import react.*
import react.dom.aria.AriaHasPopup
import react.dom.aria.ariaHasPopup
import react.dom.aria.ariaLabel
import react.dom.events.MouseEvent
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.picture
import react.dom.html.ReactHTML.source
import react.router.useNavigate
import web.html.HTMLButtonElement
import web.window.WindowTarget
import web.window.window

/** Combined home properties. */
external interface HomeProps: ElidePageProps {
  // Nothing yet.
}

/** Props for a nav link on the home page. */
external interface BaseHomeNavLinkProps : PropsWithChildren {
  /** Label to show for this link. */
  var label: String

  /** Page that will be linked to. */
  var page: SitePage

  /** Function to call to trigger navigation. */
  var doNavigate: (SitePage) -> Unit

  /** Whether this link is external. */
  var external: Boolean

  /** If the link is external, the destination for the link. */
  var href: String

  /** Title to include for the link. */
  var title: String

  /** Whether this link should open in a new tab. */
  var newTab: Boolean

  /** ARIA label for the nav link. */
  var ariaLabel: String?
}

/** Properties specific to a non-icon nav link. */
external interface HomeNavLinkProps : BaseHomeNavLinkProps {
  /** Icon element to show for this link. */
  var startIcon: ElementType<*>?
}

/** Icon navigation link properties. */
external interface HomeNavLinkIconProps : BaseHomeNavLinkProps {
  /** Icon element to show for this link. */
  var icon: ElementType<*>
}

// Perform navigation steps for a home page nav link or nav link icon.
private fun doHomeNavigate(event: MouseEvent<HTMLButtonElement, *>, props: BaseHomeNavLinkProps) {
  event.stopPropagation()
  event.preventDefault()
  if (props.external) {
    window.open(
      props.href,
      if (props.newTab) WindowTarget._blank else WindowTarget._self,
    )
  } else {
    props.doNavigate(props.page)
  }
}

/** Implements a home-page navigation link. */
val HomeNavLink = FC<HomeNavLinkProps> {
  Button {
    title = it.title
    variant = ButtonVariant.text
    className = ClassName("elide-page__home-nav-link")
    if (it.external) {
      href = it.href
    }

    sx {
      marginRight = 1.rem
      fontSize = 1.rem
      textTransform = "none".unsafeCast<TextTransform>()
      fontWeight = 600.unsafeCast<FontWeight>()
    }

    if (it.startIcon != null) {
      val props = it
      startIcon = Fragment.create {
        props.startIcon!!()
      }
    }

    if (!it.external) {
      onClick = { event ->
        doHomeNavigate(event, it)
      }
    }
    +it.label
  }
}

/** Implements an icon-based home navigation link. */
val HomeNavIcon = FC<HomeNavLinkIconProps> {
  IconButton {
    title = it.title
    className = ClassName("elide-page__home-nav-link nav-link-icon")

    val buttonAriaLabel = it.ariaLabel
    if (!buttonAriaLabel.isNullOrBlank()) {
      ariaLabel = buttonAriaLabel
      ariaHasPopup = AriaHasPopup.`false`
    }

    sx {

    }

    onClick = { event ->
      doHomeNavigate(event, it)
    }

    it.icon()
  }
}

/** Homepage component for the Elide site. */
val Home = FC<HomeProps> {
  val navigator = useNavigate()
  val themeCtx by useContext(ThemeContext)
  val (codeSampleTabState, setCodeSampleTabState) = useState("server.kt")
  val isDarkMode = (themeCtx as ThemePackage).mode == PaletteMode.dark

  val themeClass = if (isDarkMode) {
    "elide-theme__dark"
  } else {
    "elide-theme__light"
  }

  // internal link navigation handler
  val doLinkNavigate: (SitePage) -> Unit = { page ->
    navigator(page.path)
  }

  main {
    className = ClassName("elide-site-page fullbleed elide-page__home $themeClass")

    header {
      className = ClassName("elide-page__home-header elide-noselect")

      img {
        className = ClassName("elide-noselect")
        alt = "Elide Logo"
        height = 36.0
        src = if (isDarkMode) Assets.Images.logoGray else Assets.Images.stampColor
      }

      Typography {
        component = h1
        variant = TypographyVariant.h1
        className = ClassName("elide-noselect elide-titletext elide-logotext")

        +"Elide"
      }

      if (!it.mobile) {
        nav {
          className = ClassName("elide-page__home-nav")

          HomeNavLink {
            label = "Getting Started"
            page = GettingStarted
            title = "Get started with Elide"
            doNavigate = doLinkNavigate
          }

          HomeNavLink {
            label = "Samples"
            page = Samples
            title = "Code samples for different use cases"
            doNavigate = doLinkNavigate
          }

          HomeNavLink {
            label = "Tools"
            page = Tooling
            title = "Tooling and plugins"
            doNavigate = doLinkNavigate
          }

          HomeNavLink {
            label = "API Docs"
            external = true
            href = "https://docs.elide.dev"
            newTab = true
            title = "Elide API docs"
          }

          div {
            className = ClassName("elide-page__home-nav-right")

            HomeNavIcon {
              label = "API Docs"
              icon = MenuBook
              external = true
              href = "https://docs.elide.dev"
            }

            HomeNavIcon {
              label = "GitHub"
              icon = GitHub
              external = true
              href = "https://github.com/elide-dev"
            }
          }
        }
      } else {
        // mobile: render menu instead of nav bar
      }
    }

    div {
      className = ClassName("elide-page__home-content")

      section {
        className = ClassName("elide-page__home-column elide-page__home-column-left")

        Typography {
          component = h2
          variant = TypographyVariant.h2
          className = ClassName(
            "elide-page__home-masthead-text elide-titletext masthead-text masthead-text__title"
          )

          style = jso {
            lineHeight = 1.2.unsafeCast<LineHeight>()
            fontWeight = 600.unsafeCast<FontWeight>()
            letterSpacing = (-0.00833).em
          }

          span {
            +"Imagine what you could build if "
          }
          span {
            className = ClassName("masthead-text__highlight")
            +"language barriers didn't exist"
          }
        }

        Typography {
          component = h3
          variant = TypographyVariant.subtitle1
          className = ClassName("elide-titletext masthead-text__subtitle")

          span {
            className = ClassName("elide-brand-text masthead-text")

            +"Elide"
          }

          span {
            className = ClassName("masthead-text")
            +" is a framework and runtime designed for a "
          }
          span {
            className = ClassName("masthead-text masthead-text__highlight")
            + "polyglot future"
          }
        }

        div {
          className = ClassName("elide-page__home-masthead-cta")

          Button {
            variant = ButtonVariant.outlined
            color = ButtonColor.primary
            className = ClassName("elide-page__home-masthead-cta-button")

            sx {
              marginRight = 1.rem
              fontSize = 1.rem
              textTransform = "none".unsafeCast<TextTransform>()
            }

            onClick = {
              doLinkNavigate(GettingStarted)
            }

            +"Try it out"
          }
        }
      }

      section {
        className = ClassName("elide-page__home-column elide-page__home-column-right mono")

        NoSsr {
          Typography {
            className = ClassName("elide-noselect")
            component = span
            variant = TypographyVariant.h5

            sx {
              fontWeight = FontWeight.bold
              fontSize = 1.rem
              color = Color("white")
              fontFamily = "\"JetBrains Mono\", ui-monospace, monospace".unsafeCast<FontFamily>()
            }

            +"Sample: Kotlin / JavaScript SSR"
          }

          div {
            className = ClassName("elide-page__home-codesample-container")

            Box {
              sx {
                borderBottom = 1.unsafeCast<BorderBottom>()
                borderColor = "divider".unsafeCast<BorderColor>()
                width = 100.pct
              }
              Tabs {
                value = codeSampleTabState
                textColor = TabsTextColor.inherit
                indicatorColor = "inherit".unsafeCast<TabsIndicatorColor>()
                onChange = { _, newValue ->
                  setCodeSampleTabState(newValue as String)
                }
                sx {
                  width = 100.pct
                }

                Tab {
                  className = ClassName("elide-page__home-codesample-tab mono")
                  label = ReactNode("server.kt")
                  value = "server.kt"
                }

                Tab {
                  className = ClassName("elide-page__home-codesample-tab mono")
                  label = ReactNode("ssr.mjs")
                  value = "ssr.mjs"
                }
              }
            }

            CodeSample {
              className = ClassName("elide-page__home-codesample")
              style = SyntaxThemeTomorrowNight
              customStyle = jso {
                background = "transparent"
                backgroundColor = "transparent"
                width = "100%"
              }

              when (codeSampleTabState) {
                "server.kt" -> {
                  language = SyntaxLanguage.KOTLIN

                  // language=kotlin
                  +"""
              /** Props structure (can originate from a shared code unit). */
              @Props data class HelloProps (
                val name: String
              )

              /** Render a page using JavaScript SSR from Kotlin. */
              @Page class Index : PageWithProps<HelloProps>() {
                /** Calculate shared SSR props. */
                override suspend fun props(state: RequestState) =
                  HelloProps(name = "Elide")

                /** Render the root page. */
                @Get("/")
                suspend fun index(request: HttpRequest<*>) = ssr(request) {
                  head {
                    // 👇 Elide will package & serve your client-side assets.
                    script("/scripts/ui.js", defer = true)
                  }
                  body {
                    // 👇 Elide can also dispatch your server-side JS.
                    injectSSR(this@Index, request)
                  }
                }
              }
            """.trimIndent()
                }
                "ssr.mjs" -> {
                  language = SyntaxLanguage.JAVASCRIPT

                  // language=javascript
                  +"""
                  export default {
                    /**
                     * Entrypoint for an SSR render call. When `injectSSR` is called
                     * in `server.kt`, this function is dispatched within the JS VM,
                     * just like it would be in Node.
                     *
                     * The `context` parameter is the value provided by the `props`
                     * method in `server.kt`.
                     * 
                     * @param request Fetch-compliant Request object.
                     * @param context Context and props from the server.
                     * @param responder Emit chunks of HTML to the client.
                     * @return Promise to complete the response.
                     */
                    async render(request, context, responder) {
                      responder({
                        // `fin=true` indicates that we are done rendering.
                        fin: true,

                        // emit a chunk of HTML to the render stream.
                        content: ```
                          <b>Hello, ${'$'}{context.name || "stranger"}!</b>
                        ```
                      });
                    }
                  }
                """.trimIndent()
                }

                else -> error("Unrecognized code sample name: $codeSampleTabState")
              }
            }
          }
        }

        div {
          className = ClassName("elide-page__home-codesample-container-bottom")

          picture {
            source {
              type = "image/avif"
              srcSet = "/images/artwork/astronaut-mast@2x.avif 2x, /images/artwork/astronaut-mast@1x.avif 1x"
            }
            source {
              type = "image/webp"
              srcSet = "/images/artwork/astronaut-mast@2x.webp 2x, /images/artwork/astronaut-mast@1x.webp 1x"
            }
            source {
              type = "image/png"
              srcSet = "/images/artwork/astronaut-mast@2x.png 2x, /images/artwork/astronaut-mast@1x.png 1x"
            }

            img {
              alt = "Artwork: An astronaut floating in an Elide-themed nebula"
              srcSet = "/images/artwork/astronaut-mast@2x.png 2x, /images/artwork/astronaut-mast@1x.png 1x"
              src = "/images/artwork/astronaut-mast@1x.png"
            }
          }
        }
      }
    }

    footer {
      className = ClassName("elide-footer")

      NoSsr {
        div {
          className = ClassName("elide-page__home-footer")
          +"Made with ❤️ in California"
        }
      }

      div {
        className = ClassName("elide-page__home-legal")

        Link {
          href = "/legal/privacy"
          title = "Privacy policy for Elide"
          +"Privacy"
        }
        Link {
          href = "/legal/license"
          title = "Licensing for Elide"
          +"License"
        }
      }
    }
  }
}
