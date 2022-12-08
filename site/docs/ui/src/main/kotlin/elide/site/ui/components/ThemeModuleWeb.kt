package elide.site.ui.components

import elide.site.ui.theme.Themes
import mui.material.CssBaseline
import mui.material.styles.Theme
import mui.material.styles.ThemeProvider
import react.*
import kotlinx.browser.window
import kotlinx.browser.document

/** Get the active [Themes.Mode] from the browser. */
//private fun currentTheme(): Themes.Mode = if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
//  Themes.Mode.DARK
//} else {
//  Themes.Mode.LIGHT
//}

val ThemeModuleWeb = FC<PropsWithChildren> { props ->
  val current = Themes.Light
  val state = useState(current)
  val (theme, _) = state
//  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", {
//    updater(currentTheme().theme)
//  })
//  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", {
//    val themeName = currentTheme().name.lowercase()
//    document.querySelector("link[sizes~='any']")?.setAttribute(
//      "href",
//      "/images/favicon.svg?v=$themeName",
//    )
//  })

  ThemeContext(state) {
    ThemeProvider {
      this.theme = theme

      CssBaseline()
      +props.children
    }
  }
}
