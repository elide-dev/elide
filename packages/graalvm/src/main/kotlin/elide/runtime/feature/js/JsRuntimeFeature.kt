@file:Suppress("DEPRECATION")

package elide.runtime.feature.js

import com.oracle.svm.core.annotate.AutomaticFeature
import elide.annotations.internal.VMFeature
import elide.runtime.feature.FrameworkFeature
import org.graalvm.nativeimage.hosted.Feature


/** GraalVM feature which enables reflection required for the Elide JavaScript guest runtime. */
@VMFeature
@AutomaticFeature
internal class JsRuntimeFeature : FrameworkFeature {
  private companion object {
    private val registeredPackages: List<String> = listOf(
        "struct.map",
    )

    private val registeredIntrinsics: List<String> = listOf(
      // Core Intrinsics
      "base64.Base64Intrinsic",
      "console.ConsoleIntrinsic",

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
