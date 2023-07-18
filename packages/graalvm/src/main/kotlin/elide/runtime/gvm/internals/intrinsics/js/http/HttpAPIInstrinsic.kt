package elide.runtime.gvm.internals.intrinsics.js.http

import elide.annotations.Inject
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.js.http.HttpAPI
import elide.runtime.intrinsics.js.http.Server
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value

@Intrinsic internal class HttpAPIInstrinsic : HttpAPI, AbstractJsIntrinsic() {
  @Inject private lateinit var contextManager: ContextManager<Context, Context.Builder>

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount 'http' module
    bindings[GLOBAL_HTTP_SYMBOL] = this
  }

  override fun createServer(
    options: Any?,
    requestListener: Value?,
  ): Server {
    require(requestListener == null || requestListener.canExecute()) {
      "Request listener must be a function if present"
    }

    return ServerIntrinsic { req, res ->
      contextManager.acquire { requestListener?.executeVoid(req, res) }
    }
  }

  private companion object {
    const val GLOBAL_HTTP = "__elide_js_http"

    val GLOBAL_HTTP_SYMBOL = GLOBAL_HTTP.asJsSymbol()
  }
}
