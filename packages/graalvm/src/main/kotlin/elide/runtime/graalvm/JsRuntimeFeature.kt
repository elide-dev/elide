package elide.runtime.graalvm

import org.graalvm.nativeimage.hosted.Feature
import com.google.cloud.nativeimage.features.NativeImageUtils.registerClassForReflection
import com.oracle.svm.core.annotate.AutomaticFeature

/** Feature that registers JS Runtime symbols for safe Polyglot access. */
@Suppress("DEPRECATION")
@AutomaticFeature
public class JsRuntimeFeature : Feature {
  /**
   * Register types which must be callable via reflection or the Polyglot API within the Elide JS Runtime.
   *
   * @param access Before-analysis info for a given GraalVM image.
   */
  private fun registerJsRuntimeTypes(access: Feature.BeforeAnalysisAccess) {
    registerClassForReflection(
      access,
      "elide.runtime.graalvm.JsRuntime",
    )
    registerClassForReflection(access, "elide.runtime.graalvm.JsRuntime${'$'}ExecutionInputs")
  }

  /** @inheritDoc */
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    registerJsRuntimeTypes(access)
  }
}
