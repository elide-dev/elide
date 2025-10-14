/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.version

public const val ELIDE_VERSION_TAG_ALPHA: String = "alpha"
public const val ELIDE_VERSION_TAG_BETA: String = "beta"
public const val ELIDE_VERSION_TAG_RC: String = "rc"
public const val ELIDE_VERSION_TAG_DEV: String = "dev"

/** Simple version number component; never null, never more than 100,000. */
public typealias VersionNumberComponent = UInt

/** Types of releases. */
public enum class ElideReleaseType (public val rank: UInt, public val tag: String? = null) {
  DEV(0u, ELIDE_VERSION_TAG_DEV),
  ALPHA(1u, ELIDE_VERSION_TAG_ALPHA),
  BETA(2u, ELIDE_VERSION_TAG_BETA),
  RC(3u, ELIDE_VERSION_TAG_RC),
  STABLE(4u);

  /** @return Whether a given release type is compatible with this one. */
  public fun satisfies(other: ElideReleaseType): Boolean = this.rank >= other.rank
}

/**
 * # Elide Version
 */
public interface ElideVersion: Comparable<ElideVersion> {
  /** String form of Elide's own version. */
  public val asString: String

  /** @return Type of Elide release. */
  public fun type(): ElideReleaseType

  /** @return e.g. `6` for `alpha6`, or `null` if this release is not a `alpha` release. */
  public fun alpha(): VersionNumberComponent?

  /** @return e.g. `6` for `beta6`, or `null` if this release is not a `beta` release. */
  public fun beta(): VersionNumberComponent?

  /** @return e.g. `1` for `rc1`, or `null` if this release is not a release candidate. */
  public fun releaseCandidate(): VersionNumberComponent?

  public companion object {
    /** Active version, known at build-time. */
    public val self: ElideVersion = ElideVersionInfo(ELIDE_VERSION)
  }
}

// Implementation of Elide's version info.
public expect class ElideVersionInfo : ElideVersion {
  internal constructor(asString: String)
}
