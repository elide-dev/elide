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

package elide.runtime.gvm.internals

import io.micronaut.core.version.SemanticVersion
import org.graalvm.nativeimage.ImageInfo

/**
 * Represents a hard-coded JS Runtime property.
 *
 * @param symbol Symbol to use for the VM property when passing it to a new context.
 * @param staticValue Value for this property.
 */
public data class VMStaticProperty internal constructor (
  override val symbol: String,
  val staticValue: String,
): VMProperty {
  public companion object {
    private const val ENABLED_TRUE = "true"
    private const val DISABLED_FALSE = "false"

    private val svmVersionMap = sortedMapOf(
      "35" to "23.1.0",
    )

    private fun currentVersion(): SemanticVersion? {
      // example: 20.0.2+9-jvmci-23.0-b14
      return System.getProperty("java.vm.version").let { vmVersion ->
        when {
          // running on JVM
          vmVersion.contains("jvmci") -> parseSemanticVersion(vmVersion.split("-")[2])

          // in a native image, we'll need to translate the SVM release to a known SDK release
          ImageInfo.inImageCode() -> parseSemanticVersion(
            requireNotNull(svmVersionMap[vmVersion.split("+").last()]) {
              "SVM version not registered: $vmVersion"
            }
          )

          // dunno what version
          else -> null
        }
      }
    }

    @JvmStatic private fun parseSemanticVersion(version: String): SemanticVersion {
      return when (version.count { it == '.' }) {
        2 -> SemanticVersion(version)
        1 -> SemanticVersion("$version.0")
        else -> error("Failed to parse semantic version: $version")
      }
    }

    /** Test a parsed GraalVM version against the provided [version]. */
    @JvmStatic private fun testGraalVMVersion(
      version: String,
      name: String,
      state: Boolean,
      alwaysProvide: Boolean = false,
      check: (SemanticVersion, SemanticVersion) -> Boolean,
    ): VMStaticProperty? {
      return parseSemanticVersion(version).let { requirement ->
        val current = currentVersion()
          ?: return null  // can't determine version safely

        if (check(requirement, current)) {
          if (state) {
            active(name)
          } else {
            inactive(name)
          }
        } else if (alwaysProvide) {
          inactive(name)
        } else null
      }
    }

    /** @return Active setting. */
    @JvmStatic public fun of(name: String, value: String): VMStaticProperty = VMStaticProperty(name, value)

    /** @return Active setting. */
    @JvmStatic public fun active(name: String): VMStaticProperty = VMStaticProperty(name, ENABLED_TRUE)

    /** @return Active setting. */
    @JvmStatic public fun inactive(name: String): VMStaticProperty = VMStaticProperty(name, DISABLED_FALSE)

    /** @return Active setting if the current GraalVM is at least [version]. */
    @JvmStatic public fun whenAtLeast(
      version: String,
      name: String,
      state: Boolean,
      alwaysProvide: Boolean = false,
      check: (SemanticVersion, SemanticVersion) -> Boolean,
    ): VMStaticProperty? {
      return testGraalVMVersion(version, name, state, alwaysProvide, check)
    }

    /** @return Setting if the current GraalVM is at most [version]; by default, it is otherwise withheld. */
    @JvmStatic public fun whenAtMost(
      version: String,
      name: String,
      state: Boolean,
      alwaysProvide: Boolean = false,
      check: (SemanticVersion, SemanticVersion) -> Boolean,
    ): VMStaticProperty? {
      return testGraalVMVersion(version, name, state, alwaysProvide, check)
    }

    /** @return Setting if the current GraalVM is at lease [version]; by default, it is otherwise withheld. */
    @JvmStatic public fun activeWhenAtLeast(
      version: String,
      name: String,
      alwaysProvide: Boolean = false,
    ): VMStaticProperty? {
      return whenAtLeast(version, name, true, alwaysProvide) { requirement, active ->
        active >= requirement
      }
    }

    /** @return Active setting if the current GraalVM is at most [version]; otherwise, withheld. */
    @JvmStatic public fun activeWhenAtMost(
      version: String,
      name: String,
      alwaysProvide: Boolean = false,
    ): VMStaticProperty? {
      return whenAtLeast(version, name, true, alwaysProvide) { requirement, active ->
        active <= requirement
      }
    }

    /** @return Inactive setting if the current GraalVM is at least [version]; otherwise, withheld. */
    @JvmStatic public fun inactiveWhenAtLeast(
      version: String,
      name: String,
      alwaysProvide: Boolean = false,
    ): VMStaticProperty? {
      return whenAtLeast(version, name, false, alwaysProvide) { requirement, active ->
        active >= requirement
      }
    }

    /** @return Active setting if the current GraalVM is at most [version]; otherwise, withheld. */
    @JvmStatic public fun inactiveWhenAtMost(
      version: String,
      name: String,
      alwaysProvide: Boolean = false,
    ): VMStaticProperty? {
      return whenAtLeast(version, name, false, alwaysProvide) { requirement, active ->
        active <= requirement
      }
    }
  }

  override fun value(): String = staticValue
}
