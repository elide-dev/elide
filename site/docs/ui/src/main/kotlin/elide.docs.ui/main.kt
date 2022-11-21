@file:Suppress("MatchingDeclarationName")

package elide.docs.ui

import react.Fragment
import react.create
import elide.js.ssr.boot
import react.Props

external interface AppProps: Props {
  var name: String?
}

fun main() = boot<AppProps> { bootProps ->
  Fragment.create() {
    SampleApp {
      message = "Hello, ${bootProps?.name ?: "Elide"}! This page was served over Hybrid SSR."
    }
  }
}
