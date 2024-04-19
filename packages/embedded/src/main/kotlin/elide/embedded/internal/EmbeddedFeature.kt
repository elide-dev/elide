package elide.embedded.internal

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.RuntimeReflection

/**
 * A GraalVM build-time feature that registers the guest-interop classes for reflection, e.g. [ImmediateResponse].
 */
internal class EmbeddedFeature : Feature {
  override fun getDescription(): String {
    return "Registers embedded runtime classes for reflection"
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)

    RuntimeReflection.register(ImmediateResponse::class.java)
    RuntimeReflection.registerAllDeclaredConstructors(ImmediateResponse::class.java)
    RuntimeReflection.registerForReflectiveInstantiation(ImmediateResponse::class.java)
  }
}
