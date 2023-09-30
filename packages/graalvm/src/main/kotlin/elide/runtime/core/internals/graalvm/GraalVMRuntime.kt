package elide.runtime.core.internals.graalvm

import org.graalvm.nativeimage.ImageInfo
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.HostRuntime
import elide.runtime.core.Version
import elide.runtime.core.internals.graalvm.GraalVMRuntime.Companion.VARIANT_JVM
import elide.runtime.core.internals.graalvm.GraalVMRuntime.Companion.VARIANT_NATIVE

/**
 * Implementation of the [HostRuntime] information for GraalVM-based engines.
 *
 * A GraalVM Runtime has two known variants: ["native"][VARIANT_NATIVE], when running from a Native Image, and
 * ["jvm"][VARIANT_JVM], when running on a regular GraalVM JDK.
 *
 * When running in JVM mode, the [version] is resolved from the host JDK, while on a Native Image, the version is
 * translated from the current SubstrateVM release.
 *
 * @see isNativeImage
 */
@DelicateElideApi public class GraalVMRuntime(
  override val version: Version? = resolveVersion(),
  override val variant: String? = resolveVariant(),
) : HostRuntime {
  override val name: String = RUNTIME_NAME

  /** Whether this runtime is being executed from native compiled code. */
  public inline val native: Boolean get() = variant == VARIANT_NATIVE

  public companion object {
    /** Name of the system property that provides the current JVM version. */
    private const val SYSTEM_JVM_VERSION: String = "java.vm.version"

    /**
     * Internal map used to resolve a known GraalVM version from a SubstrateVM version. The keys represent SubstrateVM
     * versions and the values are the corresponding GraalVM release.
     */
    private val SubstrateVersionMap = sortedMapOf(
      "35" to "23.1.0",
    )

    /** Constant value used to provide the [HostRuntime.name] property for the [GraalVMRuntime]. */
    internal const val RUNTIME_NAME: String = "graalvm"

    /** Constant value representing a [GraalVMRuntime] variant running from a Native Image.*/
    public const val VARIANT_NATIVE: String = "native"

    /** Constant value representing a [GraalVMRuntime] variant running on a JDK.*/
    public const val VARIANT_JVM: String = "jvm"

    /** Version constant for GraalVM 23.0 */
    public val GVM_23: Version = Version(23)

    /** Version constant for GraalVM 23.1 */
    public val GVM_23_1: Version = Version(23, 1)

    /**
     * Detect the current runtime version and return it as a comparable [Version] object. When running from a
     * Native Image, the resolved version will be mapped to a known GraalVM version.
     *
     * @param source The source of the version to be resolved, defaults to using system properties.
     * @return The version of the current runtime, or `null` if the "java.vm.version" property value is not recognized
     * as a valid version or the current SubstrateVM version cannot be mapped to a known GraalVM version.
     */
    internal fun resolveVersion(source: String = System.getProperty(SYSTEM_JVM_VERSION)): Version? {
      // in a native image, we'll need to translate the SVM release to a known SDK release
      if (ImageInfo.inImageCode()) source.split("+").lastOrNull()?.let { svm ->
        return Version.parse(SubstrateVersionMap[svm] ?: return null)
      }

      // running on GraalVM JVM (e.g. 20.0.2+9-jvmci-23.0-b14)
      if (source.contains("jvmci")) source.split("-").getOrNull(2)?.let { jvm ->
        return Version.parse(jvm)
      }

      // unknown version
      return null
    }

    /**
     * Detect the active runtime variant: [VARIANT_NATIVE] if running from a Native Image, or [VARIANT_JVM] if running
     * on a GraalVM JDK.
     *
     * @return The [VARIANT_NATIVE] constant if running from a native image, or [VARIANT_JVM] if running on a JDK.
     */
    internal fun resolveVariant(): String {
      if (ImageInfo.inImageCode()) return VARIANT_NATIVE
      return VARIANT_JVM
    }
  }
}

/** Whether this runtime is hosted from a GraalVM Native Image. */
@DelicateElideApi public inline val HostRuntime.isNativeImage: Boolean
  get() = this is GraalVMRuntime && this.native