package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.js.*
import org.graalvm.polyglot.Value

/**
 * # Fetch API
 *
 * TBD.
 */
@Intrinsic internal class FetchIntrinsic : FetchAPI, AbstractJsIntrinsic() {
  internal companion object {
    /** Global where the fetch method is available. */
    private const val GLOBAL_FETCH = "fetch"

    /** Global where the `Request` constructor is mounted. */
    private const val GLOBAL_REQUEST = "Request"

    /** Global where the `Response` constructor is mounted. */
    private const val GLOBAL_RESPONSE = "Response"

    /** Global where the `Headers` constructor is mounted. */
    private const val GLOBAL_HEADERS = "Headers"

    // `Headers` intrinsic symbol.
    private val HEADERS_SYMBOL = GLOBAL_HEADERS.asJsSymbol()

    // `Request` intrinsic symbol.
    private val REQUEST_SYMBOL = GLOBAL_REQUEST.asJsSymbol()

    // `Response` intrinsic symbol.
    private val RESPONSE_SYMBOL = GLOBAL_RESPONSE.asJsSymbol()

    // `fetch` intrinsic symbol.
    private val FETCH_SYMBOL = GLOBAL_FETCH.asJsSymbol()
  }

  /** @inheritDoc */
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount `Headers`
    bindings[HEADERS_SYMBOL] = FetchHeadersIntrinsic::class.java

    // mount `Request`
    bindings[REQUEST_SYMBOL] = FetchRequestIntrinsic::class.java

    // mount `Response`
    bindings[RESPONSE_SYMBOL] = FetchResponseIntrinsic::class.java

    // mount `fetch` method
    bindings[FETCH_SYMBOL] = { request: Value ->
      handleFetch(request)
    }
  }

  // Handle a polymorphic VM-originating request to `fetch`.
  @Polyglot private fun handleFetch(request: Value): JsPromise<FetchResponse> = when {
    // invocation with a plain URL string
    request.isString -> fetch(request.asString())

    // invocation with a mocked `Request`
    request.isHostObject && request.asHostObject<Any>() is FetchRequest -> fetch(
      request.asHostObject() as FetchRequest
    )

    // invocation with a mocked `Request`
    request.isHostObject && request.asHostObject<Any>() is URL -> fetch(
      request.asHostObject() as URL
    )

    else -> error("Unsupported invocation of `fetch`")
  }

  /** @inheritDoc */
  override fun fetch(url: String): JsPromise<FetchResponse> = fetch(
    FetchRequestIntrinsic(url)
  )

  /** @inheritDoc */
  override fun fetch(request: FetchRequest): JsPromise<FetchResponse> {
    TODO("Fetch is not implemented yet")
  }

  override fun fetch(url: URL): JsPromise<FetchResponse> {
    TODO("Not yet implemented")
  }
}
