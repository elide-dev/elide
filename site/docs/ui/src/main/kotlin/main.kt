@file:Suppress("MatchingDeclarationName")

import react.Fragment
import react.create
import elide.js.ssr.boot
import elide.site.abstract.SitePage
import elide.site.pages.Home
import elide.site.ui.ElideSite
import elide.site.ui.components.ThemeModuleWeb
import elide.site.ui.initializeSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import react.Props
import react.router.dom.BrowserRouter
import web.dom.document
import web.events.EventType
import web.location.location
import kotlin.coroutines.CoroutineContext

/** Application-level properties. */
external interface AppProps: Props {
  var page: String?
}

/** Application entrypoint. */
class ElideSiteApplication (
  private val enableDebug: Boolean,
  private val currentPage: SitePage?,
) : CoroutineScope {
  private var job = Job()
  override val coroutineContext: CoroutineContext
    get() = job

  // Start the application.
  internal fun start() {
    // initialize the site
    initializeSite(enableDebug)
    render()
  }

  // Render the site.
  private fun render() = boot<AppProps> {
    Fragment.create {
      BrowserRouter {
        ThemeModuleWeb {
          ElideSite {
            mobile = false
            page = currentPage?.name ?: "not-found"
            full = currentPage?.name == Home.name
          }
        }
      }
    }
  }
}

fun main() {
  // resolve basic state
  val enableDebug = location.hostname == "localhost"
  val currentPage = elide.site.ElideSite.pages.find {
    it.path == location.pathname || location.pathname != "/" && it.path.startsWith(location.pathname)
  }

  // add content-loaded listener, start application within co-routine context
  document.addEventListener(EventType("DOMContentLoaded"), {
    ElideSiteApplication(
      enableDebug = enableDebug,
      currentPage = currentPage,
    ).start()
  })
}
