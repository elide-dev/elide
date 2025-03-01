package elide.tool.engine

import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import elide.annotations.engine.VMFeature

@VMFeature class MacLinkageFeature : Feature {
  private val macFrameworks = listOf(
    "Security",
    "SystemConfiguration",
  )

  override fun getDescription(): String = "Linkage configuration for modern macOS"

  override fun isInConfiguration(access: IsInConfigurationAccess?): Boolean {
    return System.getProperty("os.name").lowercase().trim().contains("mac")
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess?) {
    (access as BeforeAnalysisAccessImpl).nativeLibraries.let { libs ->
      macFrameworks.forEach { framework ->
        libs.addDynamicNonJniLibrary("-framework $framework")
      }
    }
  }
}
