@file:Suppress("MatchingDeclarationName")

import react.Fragment
import react.create
import elide.js.ssr.boot
import elide.site.ui.ElideSite
import elide.site.ui.components.ThemeModuleWeb
import react.Props
import react.router.dom.BrowserRouter

external interface AppProps: Props {
  var page: String?
}

fun main() = boot<AppProps> {
  Fragment.create() {
    BrowserRouter {
      ThemeModuleWeb {
        ElideSite {
          page = it?.page
          mobile = false
        }
      }
    }
  }
}
