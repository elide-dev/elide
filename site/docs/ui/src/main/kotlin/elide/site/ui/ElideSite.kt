package elide.site.ui

import csstype.Auto.auto
import csstype.ClassName
import csstype.Display
import csstype.GridTemplateAreas
import csstype.array
import elide.site.ui.components.*
import elide.site.ui.theme.Area
import elide.site.ui.theme.Sizes
import js.core.jso
import mui.system.Box
import mui.system.sx
import react.router.useLocation

// Whether to emit debug logs to the console.
private var debugLogging = false

/** Info provided by sidebar context. */
external interface SidebarState {
  /** Whether the sidebar is open. */
  var open: Boolean

  /** Toggle the sidebar open or closed; passing `null` reverses the current state. */
  var setOpen: (Boolean?) -> Unit
}

/** Info provided by site-wide context. */

external interface ElideSiteProps : react.Props {
  /** Active page path. */
  var page: String?
}

/** Page-level properties. */
external interface ElidePageProps : react.Props {
  /** Active page path. */
  var page: String?

  /** Whether we are rendering in full-bleed mode. */
  var full: Boolean

  /** Whether we are rendering in mobile mode. */
  var mobile: Boolean
}

/** Site-wide props. */
external interface SiteWideState : react.Props {
  /** Sidebar state. */
  var sidebar: SidebarState

  /** Mobile render mode. */
  var mobile: Boolean

  /** Full-bleed render mode. */
  var full: Boolean
}

/** Context component for site-wide info. */
val SiteWideContextComponent = react.createContext<SiteWideState>(jso())

/** Site-wide context component. */
@Suppress("unused") val ElideSiteContext = SiteWideContextComponent.Consumer

// Toggle function for the sidebar.
private val sidebarToggler: (react.StateSetter<Boolean>, Boolean, Boolean?) -> Unit = { updater, showing, shouldShow ->
  updater(shouldShow ?: !showing)
}

// Emit a debug log before we've acquired a logger.
private fun earlyDebugLog(vararg values: Any?) {
  if (debugLogging) console.log("[elide:site]", *values)
}

// Enabled languages.
private val enabledLanguages = listOf(
  SyntaxLanguage.JAVASCRIPT,
  SyntaxLanguage.BASH,
  SyntaxLanguage.KOTLIN,
  SyntaxLanguage.GROOVY,
  SyntaxLanguage.PYTHON,
  SyntaxLanguage.XML,
)

/**
 * Called from the `main` function to perform early-init steps.
 *
 * @param enableDebug Whether debug logging is enabled.
 */
fun initializeSite(enableDebug: Boolean) {
  debugLogging = enableDebug

  if (enableDebug) console.info("[elide:site]", "Debug logging is enabled.")
  earlyDebugLog("Registering syntax highlighting languages", enabledLanguages)

  configureCodeSamples(enabledLanguages).then {
    earlyDebugLog("Languages registered.")
  }
}

/**
 * Determine whether to enable full-bleed mode.
 *
 * @param location Location to use.
 * @return Whether full-bleed mode is active.
 */
private fun determineFullbleed(location: history.Location): Boolean {
  return location.pathname == "/"
}

/**
 * Determine whether to enable mobile rendering.
 *
 * @return Whether full-bleed mode is active.
 */
private fun determineMobile(): Boolean {
  return false
}

/**
 * Elide site.
 *
 * Main site component which implements the Elide website. This includes the [Header], [Content], and [Sidebar], wired
 * together with proper state.
 */
val ElideSite = react.FC<ElideSiteProps> {
  val location = useLocation()
  val (fullbleed, setFullbleed) = react.useState(determineFullbleed(location))
  val (isMobile, _) = react.useState(determineMobile())

  val (isSidebarShowing, sidebarShowingUpdater) = react.useState(fullbleed || isMobile)

  react.useEffect(listOf(location)) {
    setFullbleed(determineFullbleed(location))
  }

  Box {
    className = ClassName("elide-site-container")

    sx {
      display = Display.grid
      gridTemplateRows = array(
        Sizes.Header.Height,
        auto,
      )

      gridTemplateColumns = array(
        Sizes.Sidebar.Width,
        auto,
      )

      gridTemplateAreas = GridTemplateAreas(
        arrayOf(Area.Header, Area.Header),
        if (isMobile) arrayOf(Area.Content, Area.Content) else arrayOf(Area.Sidebar, Area.Content),
      )
    }

    val siteWideState = jso<SiteWideState> {
      sidebar = jso {
        open = isSidebarShowing
        setOpen = { shouldShow ->
          sidebarToggler(sidebarShowingUpdater, isSidebarShowing, shouldShow)
        }
      }
    }

    SiteWideContextComponent.Provider {
      value = siteWideState

      if (!fullbleed) {
        Header()
      }
      if (!fullbleed) {
        if (isMobile) Menu() else Sidebar()
      }
      Content {
        page = it.page
        full = fullbleed
        mobile = isMobile
      }
    }
  }
}
