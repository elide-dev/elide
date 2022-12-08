package elide.site.ui.components

import elide.site.ui.theme.Themes
import mui.material.CssBaseline
import mui.material.styles.ThemeProvider
import react.*

val ThemeContext = createContext<ThemeState>()

val ThemeModuleServer = FC<PropsWithChildren> { props ->
  val state = useState(Themes.Mode.LIGHT.theme)
  val (theme, _) = state

  ThemeContext(state) {
    ThemeProvider {
      this.theme = theme

      CssBaseline()
      +props.children
    }
  }
}
