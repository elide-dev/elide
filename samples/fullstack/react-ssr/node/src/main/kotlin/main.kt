
import fullstack.react.ui.SampleApp
import react.Fragment
import react.create
import react.dom.server.rawRenderToString


/** @return String-rendered SSR content from React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderContent(): String {
  return rawRenderToString(Fragment.create() {
    SampleApp {
      message = "Hello, Elide! This page was served over Hybrid SSR."
    }
  })
}
