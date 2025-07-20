/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.gvm.internals.intrinsics.js.fetch

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import java.nio.charset.StandardCharsets
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.*
import elide.vm.annotations.Polyglot

/**
 * # Fetch API
 *
 * TBD.
 */
@Intrinsic @ReflectiveAccess @Introspected internal class FetchIntrinsic : FetchAPI, AbstractJsIntrinsic() {
  internal companion object {
    /** Global where the fetch method is available. */
    private const val GLOBAL_FETCH = "fetch"

    /** Global where the `Request` constructor is mounted. */
    private const val GLOBAL_REQUEST = "Request"

    /** Global where the `Response` constructor is mounted. */
    private const val GLOBAL_RESPONSE = "Response"

    /** Global where the `Headers` constructor is mounted. */
    private const val GLOBAL_HEADERS = "Headers"
  }

  @OptIn(DelicateElideApi::class)
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount `Headers`
    bindings[GLOBAL_HEADERS.asPublicJsSymbol()] = FetchHeadersIntrinsic::class.java

    // mount `Request`
    bindings[GLOBAL_REQUEST.asPublicJsSymbol()] = ProxyInstantiable {
      val first = it.getOrNull(0)
      val second = it.getOrNull(1)

      // first parameter is expected to be a URL string or parsed URL
      val url: URLIntrinsic.URLValue = when {
        first != null && first.isProxyObject -> first.asProxyObject<URLIntrinsic.URLValue>()
        first != null && first.isString -> URLIntrinsic.URLValue.create(first.asString())
        else -> throw JsError.typeError("First parameter to Request must be a string or URL")
      }
      val options = when {
        second == null || second.isNull -> FetchRequestIntrinsic.FetchRequestOptions()
        else -> FetchRequestIntrinsic.FetchRequestOptions.from(second)
      }
      FetchRequestIntrinsic(
        targetUrl = url,
        targetMethod = options.method,
        requestHeaders = options.headers ?: FetchHeadersIntrinsic.empty(),
        bodyData = when {
          options.body == null || options.body.isNull -> null
          options.body.isString -> options.body.asString().byteInputStream(StandardCharsets.UTF_8)
          else -> throw JsError.typeError("Body must be a string or byte array")
        },
      )
    }

    // mount `Response`
    bindings[GLOBAL_RESPONSE.asPublicJsSymbol()] = ProxyInstantiable {
      val first = it.getOrNull(0)
      val second = it.getOrNull(1)
      if (first == null && second == null) {
        return@ProxyInstantiable FetchResponseIntrinsic(null, FetchResponseIntrinsic.FetchResponseOptions())
      }

      val bodyValue: Value? = when {
        first == null || first.isNull -> null
        first.isString -> first
        else -> error("Unsupported `Response` body type")
      }
      val options = when {
        second == null || second.isNull -> FetchResponseIntrinsic.FetchResponseOptions()
        else -> FetchResponseIntrinsic.FetchResponseOptions.from(second)
      }
      FetchResponseIntrinsic(
        bodyValue?.asString(),
        options,
      )
    }

    // mount `fetch` method
    bindings[GLOBAL_FETCH.asPublicJsSymbol()] = { request: Value ->
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

  override fun fetch(url: String): JsPromise<FetchResponse> = fetch(
    FetchRequestIntrinsic(url)
  )

  override fun fetch(request: FetchRequest): JsPromise<FetchResponse> = handleFetch(Value.asValue(request))

  override fun fetch(url: URL): JsPromise<FetchResponse> = handleFetch(Value.asValue(url))
}
