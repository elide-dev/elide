
import elide.frontend.ssr.SSRContext
import elide.docs.ui.SampleApp
import react.Fragment
import react.Props
import react.create
import react.dom.server.rawRenderToString

/** Props shared with the server. */
external interface HelloProps : Props {
  /** @return `Name` context value from the server. */
  fun getName(): String?
}

/** @return String-rendered SSR content from React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderContent(context: dynamic = null): String {
  return SSRContext.typed<HelloProps>(context).execute {
    rawRenderToString(Fragment.create {
      SampleApp {
        message = "Hello, ${state?.getName() ?: "Elide"}! This page was served over Hybrid SSR."
      }
    })
  }
}
