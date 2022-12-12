package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.annotations.core.Polyglot
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.js.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value

/**
 * # Fetch API
 *
 * TBD.
 */
@Intrinsic internal class FetchIntrinsic : FetchAPI, AbstractJsIntrinsic() {
  internal companion object {
    /** Global where the fetch method is available. */
    const val GLOBAL_FETCH = "fetch"

    /** Global where the `Request` constructor is mounted. */
    const val GLOBAL_REQUEST = "Request"

    /** Global where the `Response` constructor is mounted. */
    const val GLOBAL_RESPONSE = "Response"

    /** Global where the `Headers` constructor is mounted. */
    const val GLOBAL_HEADERS = "Headers"

    /** Global where the `URL` constructor is mounted. */
    const val GLOBAL_URL = "URL"

    // Create a new `Request` object.
    @Polyglot @JvmStatic private fun createRequest(input: Value): FetchRequest = when {
      input.isString -> FetchRequestIntrinsic(input.asString())
      input.isHostObject && input.asHostObject<Any>() is FetchURL ->
        FetchRequestIntrinsic(input.asHostObject() as FetchURL)
      input.isHostObject && input.asHostObject<Any>() is FetchRequest ->
        FetchRequestIntrinsic(input.asHostObject() as FetchRequest)
      else -> error(
        "Unsupported type for `Request` constructor: '${input.metaObject.metaSimpleName}'"
      )
    }

    // Create a new `Request` object.
    @Polyglot @JvmStatic private fun createResponse(input: Value? = null): FetchResponse = when (input) {
      null -> FetchResponseIntrinsic.ResponseConstructors.empty()
      else -> error("Unsupported type for `Response` constructor: '${input.metaObject.metaSimpleName}'")
    }

    // Create an empty `Headers` object.
    @Polyglot @JvmStatic private fun createHeaders(input: Value? = null): FetchHeaders = when (input) {
      null -> FetchHeadersIntrinsic.HeadersConstructors.empty()
      else -> error("Unsupported type for `Headers` constructor: '${input.metaObject.metaSimpleName}'")
    }
  }

  /** @inheritDoc */
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount `Headers` constructors
    bindings[GLOBAL_HEADERS] = ::createHeaders

    // mount `Request` constructors
    bindings[GLOBAL_REQUEST] = ::createRequest

    // mount `Response` constructors
    bindings[GLOBAL_RESPONSE] = ::createResponse

    // mount `fetch` method
    bindings[GLOBAL_FETCH] = { request: Value ->
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
    request.isHostObject && request.asHostObject<Any>() is FetchURL -> fetch(
      request.asHostObject() as FetchURL
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
}
