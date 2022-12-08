package elide.site.ui.components

import mui.material.styles.Theme

/** Site theme state. */
typealias ThemeState = react.StateInstance<Theme>

/** Context for site theme. */
val ThemeContext = react.createContext<ThemeState>()
