package elide.site.ui.components

import elide.site.ui.theme.Themes
import js.core.Object
import js.core.jso
import mui.material.CssBaseline
import mui.material.PaletteMode
import mui.material.styles.ThemeProvider

/** Theme context provider for server/static environments. */
val ThemeModuleServer = react.FC<react.PropsWithChildren> { props ->
  val state = react.useState(Object.assign(Themes.Mode.LIGHT.theme, jso<ThemePackage> {
    mode = PaletteMode.light
  }))

  val (currentTheme, _) = state

  ThemeContext(state) {
    ThemeProvider {
      theme = currentTheme

      CssBaseline()
      +props.children
    }
  }
}
