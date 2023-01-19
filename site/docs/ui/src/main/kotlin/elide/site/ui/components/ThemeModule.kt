package elide.site.ui.components

import mui.material.PaletteMode
import mui.material.styles.Theme

/** Package of a theme [mode] and [Theme] info. */
external interface ThemePackage : Theme {
  var mode: PaletteMode
}

/** Site theme state. */
typealias ThemeState = react.StateInstance<Theme>

/** Context for site theme. */
val ThemeContext = react.createContext<ThemeState>()
