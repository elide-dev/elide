/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.feature.js

import elide.annotations.internal.VMFeature
import elide.runtime.feature.EngineFeature
import elide.runtime.gvm.internals.GraalVMGuest

/** GraalVM feature which enables reflection required for the Elide JavaScript guest runtime. */
@VMFeature internal class JavaScriptFeature : EngineFeature(GraalVMGuest.JAVASCRIPT) {
  private companion object {
    private val packages: List<String> = listOf(
        "struct.map",
    ).map {
      "elide.runtime.intrinsics.js.$it"
    }

    private val intrinsics: List<String> = listOf(
      // Core Intrinsics
      "base64.Base64Intrinsic",
      "console.ConsoleIntrinsic",

      // Utility Types
      "typed.UUIDValue",
      "typed.UUIDValue${'$'}ValidUUID",

      // Web Crypto API
      "crypto.WebCryptoIntrinsic",
      "crypto.WebCryptoKey",

      // Fetch API
      "fetch.FetchIntrinsic",
      "fetch.FetchHeadersIntrinsic",
      "fetch.FetchRequestIntrinsic",
      "fetch.FetchResponseIntrinsic",

      // URL API
      "url.URLIntrinsic",
      "url.URLIntrinsic${'$'}URLValue",
      "url.URLSearchParamsIntrinsic",
      "url.URLSearchParamsIntrinsic${'$'}URLSearchParams",
      "url.URLSearchParamsIntrinsic${'$'}MutableURLSearchParams",
    ).map {
      "elide.runtime.gvm.internals.intrinsics.js.$it"
    }
  }

  override fun engineTypes(): Triple<String, String, String> = Triple(
    "elide.runtime.gvm.internals.js.JsRuntime",
    "elide.runtime.gvm.internals.js.JsExecutableScript",
    "elide.runtime.gvm.internals.js.JsInvocationBindings",
  )

  override fun implementationTypes(): List<String> = super.implementationTypes().plus(listOf(
    "elide.runtime.intrinsics.js.JsIterator",
    "elide.runtime.intrinsics.js.JsIterator${'$'}JsIteratorResult",
    "elide.runtime.intrinsics.js.JsIterator${'$'}JsIteratorImpl",
  ))

  override fun registeredPackages(): List<String> = packages

  override fun registeredIntrinsics(): List<String> = intrinsics
}
