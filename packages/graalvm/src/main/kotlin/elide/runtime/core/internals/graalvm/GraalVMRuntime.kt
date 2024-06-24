/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
      "23" to "24.1.0",
      "23.0.0" to "24.1.0",
      "22.0.1" to "24.0.1",
      "22.0.0" to "24.0.0",
      "8.1" to "24.1.0",
      "36" to "24.0.1",
      "35" to "23.1.0",
      "13.1" to "23.1.2",
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

    /** Version constant for GraalVM 24.0 */
    public val GVM_24: Version = Version(24)

    /** Version constant for GraalVM 24.0.1 */
    public val GVM_24_0_1: Version = Version(24, 0, 1)

    /** Version constant for GraalVM 24.0.1 */
    public val GVM_24_1_0: Version = Version(24, 1, 0)

    private const val positionalLts = 3
    private const val positionalNonLts = 2

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
        return Version.parse(SubstrateVersionMap[svm] ?: return GVM_24_1_0)
      }

      // running on GraalVM JVM (e.g. 20.0.2+9-jvmci-23.0-b14)
      val tag = source.lowercase().trim()
      if (source.contains("jvmci")) {
        // check for LTS version tag, for example,
        // `21.0.2+13-LTS-jvmci-23.1-b30`
        val index = if (tag.contains("lts")) positionalLts else positionalNonLts
        return source.split("-").getOrNull(index)?.let { jvm ->
          if (!jvm.contains(".")) null else Version.parse(jvm)
        } ?: source.split("+").getOrNull(0)?.let { jvm ->
          Version.parse(SubstrateVersionMap[jvm] ?: error("JVM version not mapped: $jvm"))
        }
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
