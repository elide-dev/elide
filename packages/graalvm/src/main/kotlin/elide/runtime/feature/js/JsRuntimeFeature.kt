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

import org.graalvm.nativeimage.hosted.Feature
import elide.annotations.internal.VMFeature
import elide.runtime.feature.FrameworkFeature


/** GraalVM feature which enables reflection required for the Elide JavaScript guest runtime. */
@VMFeature
internal class JsRuntimeFeature : FrameworkFeature {
  private companion object {
    private val registeredPackages: List<String> = listOf(
        "struct.map",
    )

    private val registeredIntrinsics: List<String> = listOf(
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

      // Express API
      "express.ExpressIntrinsic",
      "express.ExpressAppIntrinsic",
      "express.ExpressRequestIntrinsic",
      "express.ExpressResponseIntrinsic",
    )
  }

  // -- Interface: VM Feature -- //
  /**
   * Register types which must be callable via reflection or the Polyglot API within the Elide JS Runtime.
   *
   * @param access Before-analysis info for a given GraalVM image.
   */
  private fun registerJsRuntimeTypes(access: Feature.BeforeAnalysisAccess) {
    registerClassForReflection(access, "elide.runtime.gvm.internals.js.JsRuntime")
    registerClassForReflection(access, "elide.runtime.intrinsics.js.JsIterator")
    registerClassForReflection(access, "elide.runtime.intrinsics.js.JsIterator${'$'}JsIteratorResult")
    registerClassForReflection(access, "elide.runtime.intrinsics.js.JsIterator${'$'}JsIteratorImpl")
    registeredIntrinsics.forEach {
      registerClassForReflection(access, "elide.runtime.gvm.internals.intrinsics.js.${it}")
    }
    registeredPackages.forEach {
      registerPackageForReflection(access, "elide.runtime.intrinsics.js.$it")
    }
  }

  /** @inheritDoc */
  override fun getRequiredFeatures(): MutableList<Class<out Feature>> {
    return arrayListOf(
      elide.runtime.feature.ProtocolBuffers::class.java,
      elide.runtime.feature.VirtualFilesystem::class.java,
    )
  }

  /** @inheritDoc */
  override fun isInConfiguration(access: Feature.IsInConfigurationAccess): Boolean {
    return (
      // the JS runtime must be in the classpath
      access.findClassByName("elide.runtime.gvm.internals.js.JsRuntime") != null
    )
  }

  /** @inheritDoc */
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    registerJsRuntimeTypes(access)
  }

  /** @inheritDoc */
  override fun getDescription(): String = "Enables the Elide JS runtime"
}
