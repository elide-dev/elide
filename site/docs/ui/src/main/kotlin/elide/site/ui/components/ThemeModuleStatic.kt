package elide.site.ui.components

import elide.site.ui.theme.Themes
import mui.material.CssBaseline
import mui.material.styles.ThemeProvider
import react.*

/** Theme context provider for server/static environments. */
val ThemeModuleServer = FC<PropsWithChildren> { props ->
  val state = useState(Themes.Mode.LIGHT.theme)
  val (currentTheme, _) = state

  ThemeContext(state) {
    ThemeProvider {
      theme = currentTheme

      CssBaseline()
      +props.children
    }
  }
}
