package elide.site.ui.components

import csstype.*
import elide.site.Assets
import elide.site.ElideSite
import elide.site.ExternalLinks
import elide.site.SiteLinks
import elide.site.pages.Home
import elide.site.ui.theme.Area
import elide.site.ui.theme.Themes
import emotion.react.css
import js.core.Object
import js.core.jso
import kotlinx.browser.window
import mui.icons.material.Brightness4
import mui.icons.material.Brightness7
import mui.icons.material.GitHub
import mui.icons.material.MenuBook
import mui.material.*
import mui.material.Size
import mui.material.styles.TypographyVariant.h6
import mui.system.sx
import react.*
import react.dom.aria.AriaHasPopup.`false`
import react.dom.aria.ariaHasPopup
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img

/** Renders the main Elide site header. */
val Header = FC<Props> {
  val siteInfo = ElideSite.defaultInfo
  var themeCtx by useContext(ThemeContext)

  AppBar {
    position = AppBarPosition.fixed
    className = ClassName("gradient")

    sx {
      gridArea = Area.Header
      zIndex = integer(1_500)
    }

    Toolbar {
      Box {
        sx {
          display = Display.flex
          flexDirection = FlexDirection.row
          justifyContent = JustifyContent.flexStart
          flexGrow = number(1.0)
        }

        Link {
          title = "${siteInfo.title} Home"
          href = Home.path
          underline = LinkUnderline.none

          div {
            css {
              display = Display.flex
              cursor = Cursor.pointer
            }

            img {
              src = Assets.Images.logoGray
              alt = "${siteInfo.name} Logo"
              width = 32.0
              height = 32.0

              css {
                marginRight = 22.px
              }
            }

            Typography {
              variant = h6
              noWrap = true
              component = div
              className = ClassName("elide-titletext elide-noselect")

              +siteInfo.heading

              css {
                color = Color("#EDEDED")
              }
            }
          }
        }

        if (siteInfo.prerelease) {
          NoSsr {
            div {
              css {
                display = Display.flex
                alignItems = AlignItems.center
                justifyContent = JustifyContent.center
                height = 32.px
              }

              Chip {
                color = ChipColor.info
                title = siteInfo.prelabel
                label = ReactNode(siteInfo.prelabel)
                className = ClassName("elide-noselect")

                sx {
                  marginLeft = 15.px
                  borderRadius = 5.px
                  height = 25.px

                  @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                  val ctx = themeCtx as ThemePackage

                  color = Themes.styled(ctx.mode, light = {
                    Color("#EDEDED")
                  }, dark = {
                    Color("#333")
                  })
                }
              }
            }
          }
        }
      }

      NoSsr {
        Tooltip {
          title = ReactNode("Theme")

          Switch {
            icon = Brightness7.create()
            checkedIcon = Brightness4.create()
            checked = themeCtx == Themes.Dark
            color = SwitchColor.default
            ariaLabel = "theme"

            onChange = { _, checked ->
              val currentTheme = if (checked) Themes.Dark else Themes.Light
              val paletteMode = if (checked) PaletteMode.dark else PaletteMode.light

              themeCtx = Object.assign(currentTheme, jso<ThemePackage> {
                mode = paletteMode
              })
            }
          }
        }

        Tooltip {
          title = ReactNode("Reference Docs")

          IconButton {
            ariaLabel = "docs"
            ariaHasPopup = `false`
            size = Size.large
            color = IconButtonColor.inherit
            onClick = {
              window.open(
                SiteLinks.ReferenceDocs.kotlin,
                target = "_blank",
              )
            }

            MenuBook()
          }
        }

        Tooltip {
          title = ReactNode("Open Github")

          IconButton {
            ariaLabel = "source code"
            ariaHasPopup = `false`
            size = Size.large
            color = IconButtonColor.inherit
            onClick = {
              window.open(
                ExternalLinks.github,
                target = "_blank",
              )
            }

            GitHub()
          }
        }
      }
    }
  }
}
