@file:OptIn(ExperimentalJsExport::class)

import elide.runtime.gvm.entrypoint
import js.core.jso
import elide.runtime.ssr.RenderCallback
import elide.runtime.ssr.ServerResponse
import kotlin.js.Promise

/** @return String-rendered SSR content from Node. */
@JsExport fun renderStream(callback: RenderCallback, context: dynamic = null): Promise<*> = entrypoint {
  callback(jso<dynamic> {
    content = "<strong>Hello, streaming SSR!</strong>"
    hasContent = true
    fin = true
    status = 200
  }.unsafeCast<ServerResponse>())

  return@entrypoint Promise { accept, _ ->
    accept(Unit)
  }
}

/** @return String-rendered SSR content from Node. */
@JsExport fun renderContent(): String = entrypoint {
  "<strong>Hello, embedded SSR!</strong>"
}
