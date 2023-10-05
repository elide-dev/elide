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

package elide.runtime.core

import elide.runtime.core.Version.Companion.parse
import elide.runtime.core.Version.Range

/**
 * Represents a semantic version that can be compared with others or used to form a version range. Version objects are
 * used to enable version-aware behavior in engine plugins.
 *
 * Use [toString] if a full version string is required instead of separate components.
 *
 * @see parse
 */
@DelicateElideApi public data class Version(
  /** Major version component. */
  public val major: Int,
  /** Minor version component. */
  public val minor: Int = 0,
  /** Patch version component. */
  public val patch: Int = 0,
) : Comparable<Version> {

  /**
   * A range of [versions][Version], which can be [queried][contains] to see if a [Version] falls in a specific
   * range. Use the normal range operators or extensions to create version ranges.
   *
   * By default, a range includes every valid version (from [Zero] onwards).
   */
  @DelicateElideApi public data class Range(
    public val min: Version = Zero,
    public val max: Version? = null,
    public val includeMin: Boolean = true,
    public val includeMax: Boolean = true,
  ) {
    public operator fun contains(version: Version): Boolean {
      val minResult = version.compareTo(min)
      val maxResult = max?.let { version.compareTo(it) }

      return when (includeMin) {
        true -> when (includeMax) {
          true -> minResult >= 0 && (maxResult == null || maxResult <= 0)
          false -> minResult >= 0 && (maxResult == null || maxResult < 0)
        }

        false -> when (includeMax) {
          true -> minResult > 0 && (maxResult == null || maxResult <= 0)
          false -> minResult > 0 && (maxResult == null || maxResult < 0)
        }
      }
    }
  }

  public override operator fun compareTo(other: Version): Int {
    major.compareTo(other.major).takeIf { it != 0 }?.let { return it }
    minor.compareTo(other.minor).takeIf { it != 0 }?.let { return it }
    patch.compareTo(other.patch).takeIf { it != 0 }?.let { return it }
    return 0
  }

  override fun toString(): String {
    return "$major.$minor.$patch"
  }

  public companion object {
    /** Number of segments required in a semantic version string (major and minor, patch defaults to 0). */
    private const val MIN_SEGMENTS = 2

    /** Regular expression used to replace characters with version separators. */
    private val SpecRegex = Regex("[-_]")

    /** A placeholder "zero" version provided for convenience. */
    public val Zero: Version by lazy { Version(0, 0, 0) }


    /**
     * Parse a string [spec] and return a rich [Version] object.
     *
     * @return A parsed [Version] containing the [major], [minor], and [patch] fields.
     * @throws IllegalArgumentException if the provided [spec] is not a semantic version.
     */
    public fun parse(spec: String): Version = runCatching {
      // replace '-' and '_' with '.', then split and check for basic contraints
      val parts = spec.replace(SpecRegex, ".").split('.')
      check(parts.size >= MIN_SEGMENTS) { "Semantic version should have at least 'major' and 'minor' components" }

      // parse the digits, only major and minor components are required
      Version(
        major = parts[0].toInt(),
        minor = parts[1].toInt(),
        patch = parts.getOrNull(2)?.toInt() ?: 0,
      )
    }.getOrElse { cause ->
      throw IllegalArgumentException("Invalid semantic version '$spec'", cause)
    }
  }
}

/** Returns a [version range][Range] including this and every higher version. */
@DelicateElideApi public fun Version.andHigher(): Range {
  return Range(this)
}

/** Returns a [version range][Range] including this and every lower version. */
@DelicateElideApi public fun Version.andLower(): Range {
  return Range(max = this)
}
