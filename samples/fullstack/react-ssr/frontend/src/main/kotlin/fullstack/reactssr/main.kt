@file:Suppress("MatchingDeclarationName")

package fullstack.reactssr

import react.Fragment
import react.create
import elide.js.ssr.boot
import fullstack.reactssr.ui.SampleApp
import react.Props

external interface AppProps: Props {
  var name: String?
}

fun main() = boot<AppProps> { bootProps ->
  Fragment.create {
    SampleApp {
      message = "Hello, ${bootProps?.name ?: "Elide"}! This page was served over Hybrid SSR."
    }
  }
}
