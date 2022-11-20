@file:Suppress("DEPRECATION")

package elide.runtime.feature

import com.oracle.svm.core.annotate.AutomaticFeature
import org.graalvm.nativeimage.hosted.Feature


/** GraalVM feature which enables reflection required for the [elide.runtime.graalvm.JsRuntime]. */
@AutomaticFeature
internal class JsRuntimeFeature : FrameworkFeature {
  // -- Interface: VM Feature -- //
  /**
   * Register types which must be callable via reflection or the Polyglot API within the Elide JS Runtime.
   *
   * @param access Before-analysis info for a given GraalVM image.
   */
  private fun registerJsRuntimeTypes(access: Feature.BeforeAnalysisAccess) {
    registerClassForReflection(access, "elide.runtime.graalvm.JsRuntime")
    registerClassForReflection(access, "elide.runtime.graalvm.JsRuntime${'$'}ExecutionInputs")
  }

  /** @inheritDoc */
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    registerJsRuntimeTypes(access)
  }
}
