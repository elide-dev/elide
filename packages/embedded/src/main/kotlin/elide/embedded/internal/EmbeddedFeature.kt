package elide.embedded.internal

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess

/**  A GraalVM build-time feature that registers the guest-interop classes for reflection. */
internal class EmbeddedFeature : Feature {
  override fun getDescription(): String {
    return "Registers embedded runtime classes for reflection"
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
  }
}
