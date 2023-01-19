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
import react.*

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

external interface ElideSiteProps : Props {
  /** Active page path. */
  var page: String?

  /** Whether the site should render in mobile mode. */
  var mobile: Boolean

  /** Whether the rendered page is full-bleed. */
  var full: Boolean
}

/** Site-wide props. */
external interface SiteWideState : Props {
  /** Sidebar state. */
  var sidebar: SidebarState
}

/** Context component for site-wide info. */
val SiteWideContextComponent = createContext<SiteWideState>(jso())

/** Site-wide context component. */
@Suppress("unused") val ElideSiteContext = SiteWideContextComponent.Consumer

// Toggle function for the sidebar.
private val sidebarToggler: (StateSetter<Boolean>, Boolean, Boolean?) -> Unit = { updater, isShowing, shouldShow ->
  updater(shouldShow ?: !isShowing)
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
  SyntaxLanguage.XML,
)

/**
 * Called from the `main` function to perform early-init steps.
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
 * Elide site.
 *
 * Main site component which implements the Elide website. This includes the [Header], [Content], and [Sidebar], wired
 * together with proper state.
 */
val ElideSite = FC<ElideSiteProps> {
  val (isSidebarShowing, sidebarShowingUpdater) = useState(it.full || !it.mobile)
  val (isHeaderShowing, _) = useState(!it.full)

  Box {
    className = ClassName("elide-site-container")

    if (!it.full) {
      sx {
        display = Display.grid
        gridTemplateRows = if (isHeaderShowing) array(
          Sizes.Header.Height,
          auto,
        ) else array(
          auto,
        )

        gridTemplateColumns = if (!isSidebarShowing) array(auto) else array(
          Sizes.Sidebar.Width,
          auto,
        )

        gridTemplateAreas = GridTemplateAreas(
          arrayOf(Area.Header, Area.Header),
          if (it.mobile)
            arrayOf(Area.Content, Area.Content)
          else if (!isSidebarShowing)
            arrayOf(Area.Content)
          else
            arrayOf(Area.Sidebar, Area.Content),
        )
      }
    } else {
      sx {
        display = Display.flex
      }
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

      if (!it.full) {
        Header()
      }
      if (!it.full) {
        if (it.mobile) Menu() else Sidebar()
      }
      Content {
        page = it.page
        full = it.full
        mobile = it.mobile
      }
    }
  }
}
