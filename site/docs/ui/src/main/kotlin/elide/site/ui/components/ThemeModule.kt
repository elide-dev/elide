package elide.site.ui.components

import elide.site.ui.theme.Themes
import mui.material.CssBaseline
import mui.material.styles.Theme
import mui.material.styles.ThemeProvider
import react.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.Clock

typealias ThemeState = StateInstance<Theme>

/** Get the active [Themes.Mode] from the browser. */
private fun currentTheme(): Themes.Mode = if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
  Themes.Mode.DARK
} else {
  Themes.Mode.LIGHT
}

//val ThemeContext = createContext<ThemeState>()

//val ThemeModule = FC<PropsWithChildren> { props ->
////  val current = Themes.Mode.current().theme
//  val state = useState(Themes.Mode.LIGHT.theme)
//  val (theme, _) = state
////  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", {
////    updater(Themes.Mode.current().theme)
////  })
////  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", {
////    document.querySelector("link[sizes~='any']")?.setAttribute(
////      "href",
////      "/images/favicon.svg?b=${Clock.System.now().epochSeconds}",
////    )
////  })
//
//  ThemeContext(state) {
//    ThemeProvider {
//      this.theme = theme
//
//      CssBaseline()
//      +props.children
//    }
//  }
//}
