@file:OptIn(ExperimentalJsExport::class)

import elide.runtime.gvm.entrypoint
import js.core.jso
import elide.frontend.ssr.RenderCallback
import org.w3c.fetch.Request
import kotlin.js.Promise

/** @return String-rendered SSR content from Node. */
@JsExport fun render(request: Request, context: dynamic, callback: RenderCallback): Promise<*> = entrypoint {
  callback(jso {
    content = "<strong>Hello, streaming SSR!</strong>"
    hasContent = true
    fin = true
    status = 200
  })

  return@entrypoint Promise { accept, _ ->
    accept(Unit)
  }
}

/** @return String-rendered SSR content from Node. */
@JsExport fun renderContent(): String = entrypoint {
  "<strong>Hello, embedded SSR!</strong>"
}
