@file:Suppress("MatchingDeclarationName")

package elide.site.ui

import react.Fragment
import react.create
import elide.js.ssr.boot
import mui.material.useMediaQuery
import react.Props
import react.router.dom.BrowserRouter

external interface AppProps: Props {
  var page: String?
}

fun main() = boot<AppProps> {
  Fragment.create() {
    val mobileMode = useMediaQuery("(max-width:960px)")
    BrowserRouter {
      ElideSite {
        page = it?.page
        mobile = mobileMode
      }
    }
  }
}
