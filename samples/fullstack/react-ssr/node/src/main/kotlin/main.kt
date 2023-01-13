
import elide.frontend.ssr.SSRContext
import fullstack.react.ui.SampleApp
import org.w3c.fetch.Request
import org.w3c.fetch.Response
import react.Fragment
import react.Props
import react.create
import react.dom.server.rawRenderToString
import kotlin.js.Promise

/** Props shared with the server. */
external interface HelloProps : Props {
  /** @return `Name` context value from the server. */
  fun getName(): String?
}

/** @return String-rendered SSR content from React. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun render(request: Request, context: dynamic, responder: dynamic): Promise<Response> {
  return SSRContext.typed<HelloProps>(context).execute {
    Promise { accept, reject ->
      val rendered = rawRenderToString(Fragment.create {
        SampleApp {
          message = "Hello, ${state?.getName() ?: "Elide"}! This page was served over Hybrid SSR."
        }
      })
      responder(rendered)
      accept(Response(200))
    }
  }
}
