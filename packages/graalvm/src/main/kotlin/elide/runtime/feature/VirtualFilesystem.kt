@file:Suppress("DEPRECATION")

package elide.runtime.feature

import com.oracle.svm.core.annotate.AutomaticFeature
import elide.annotations.internal.VMFeature
import org.graalvm.nativeimage.hosted.Feature

/** GraalVM feature which enables reflection required for VFS (Virtual File System) services. */
@VMFeature
@AutomaticFeature
internal class VirtualFilesystem : FrameworkFeature {
  internal companion object {
    private val vfsClasses = listOf(
      "elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl",
    )
  }

  /** @inheritDoc */
  override fun getRequiredFeatures(): MutableList<Class<out Feature>> = arrayListOf(
    ProtocolBuffers::class.java,
  )

  /** @inheritDoc */
  override fun isInConfiguration(access: Feature.IsInConfigurationAccess): Boolean {
    return (
      // the VFS interface and embedded impl must be in the classpath
      access.findClassByName("elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl") != null
    )
  }

  /** @inheritDoc */
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    vfsClasses.forEach {
      registerClassForReflection(access, it)
    }
  }

  /** @inheritDoc */
  override fun getDescription(): String = "Configures guest VFS features"
}
