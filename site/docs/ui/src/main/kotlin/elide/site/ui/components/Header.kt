package elide.site.ui.components

import web.cssom.ClassName
import csstype.Color
import csstype.integer
import csstype.px
import elide.site.Assets
import elide.site.ElideSite
import elide.site.ExternalLinks
import elide.site.SiteLinks
import elide.site.pages.Home
import elide.site.ui.theme.Area
import elide.site.ui.theme.Themes
import js.core.Object
import js.core.jso
import kotlinx.browser.window
import mui.icons.material.Brightness4
import mui.icons.material.Brightness7
import mui.icons.material.GitHub
import mui.icons.material.MenuBook
import mui.material.AppBar
import mui.material.AppBarPosition
import mui.material.Box
import mui.material.Chip
import mui.material.ChipColor
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Link
import mui.material.LinkUnderline
import mui.material.NoSsr
import mui.material.PaletteMode
import mui.material.Size
import mui.material.Switch
import mui.material.SwitchColor
import mui.material.Toolbar
import mui.material.Tooltip
import mui.material.Typography
import mui.material.styles.TypographyVariant.h6
import mui.system.sx
import react.create
import react.dom.aria.AriaHasPopup.`false`
import react.dom.aria.ariaHasPopup
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img

/** Renders the main Elide site header. */
val Header = react.FC<react.Props> {
  val siteInfo = ElideSite.defaultInfo
  var themeCtx by react.useContext(ThemeContext)

  AppBar {
    position = AppBarPosition.fixed
    className = ClassName("gradient")

    sx {
      gridArea = Area.Header
      zIndex = integer(1_500)
    }

    Toolbar {
      Box {
        className = ClassName("elide-header__box")

        Link {
          title = "${siteInfo.title} Home"
          href = Home.path
          underline = LinkUnderline.none
          onClick = {
            it.preventDefault()
            window.setTimeout(timeout = 0, handler = {
              window.open(Home.path, "_self")
            })
          }

          div {
            className = ClassName("elide-header__logo-box")

            img {
              src = Assets.Images.logoGray
              alt = "${siteInfo.name} Logo"
              width = 32.0
              height = 32.0
            }

            Typography {
              variant = h6
              noWrap = true
              component = div
              className = ClassName("elide-titletext elide-noselect")

              +siteInfo.heading
            }
          }
        }

        if (siteInfo.prerelease) {
          NoSsr {
            div {
              className = ClassName("elide-header__prerelease")

              Chip {
                color = ChipColor.info
                title = siteInfo.prelabel
                label = react.ReactNode(siteInfo.prelabel)
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
          title = react.ReactNode("Theme")

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
          title = react.ReactNode("Reference Docs")

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
          title = react.ReactNode("Open Github")

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
