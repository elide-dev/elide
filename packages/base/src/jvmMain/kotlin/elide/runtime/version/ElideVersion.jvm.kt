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

import org.semver4j.Semver

// JVM implementation of build-time version information.
public actual class ElideVersionInfo actual constructor (
  override val asString: String,
): ElideVersion {
  private val parsedSemver: Semver by lazy {
    requireNotNull(Semver.parse(asString)) { "Failed to parse Elide version as semver" }
  }

  private val alphaRelease: VersionNumberComponent? by lazy {
    parseComponent(parsedSemver, ELIDE_VERSION_TAG_ALPHA)
  }

  private val betaRelease: VersionNumberComponent? by lazy {
    parseComponent(parsedSemver, ELIDE_VERSION_TAG_BETA)
  }

  private val rcRelease: VersionNumberComponent? by lazy {
    parseComponent(parsedSemver, ELIDE_VERSION_TAG_RC)
  }

  /** @return Elide's own build-time version as a [Semver] instance. */
  public fun asSemver(): Semver = parsedSemver

  override fun toString(): String = asString

  private fun parseComponent(semver: Semver, tag: String): VersionNumberComponent? {
    // if this version is tagged, the pre-release component will contain `<tag>X`; we need `X`.
    val segment = semver.preRelease.first()
    val match = Regex("$tag(\\d+)").find(segment)
    return match?.groupValues?.getOrNull(0)?.drop(tag.length)?.toUInt()
  }

  override fun alpha(): VersionNumberComponent? = alphaRelease

  override fun beta(): VersionNumberComponent? = betaRelease

  override fun releaseCandidate(): VersionNumberComponent? = rcRelease

  override fun type(): ElideReleaseType = when {
    alpha() != null -> ElideReleaseType.ALPHA
    beta() != null -> ElideReleaseType.BETA
    releaseCandidate() != null -> ElideReleaseType.RC
    else -> ElideReleaseType.STABLE
  }

  override fun compareTo(other: ElideVersion): Int = parsedSemver.let { semver ->
    val otherSemver = when (other) {
      is ElideVersionInfo -> other.asSemver()
      else -> requireNotNull(Semver.parse(other.asString))
    }
    when (val semverCompare = semver.compareTo(otherSemver)) {
      // if the versions are equal, we must compare pre-release tags (if any)
      0 -> require(other is ElideVersionInfo) {
        "Can't compare pre-release tags for version: $other"
      }.let {
        when (val typeCompare = type().compareTo(other.type())) {
          0 -> when (type()) {
            ElideReleaseType.ALPHA -> alpha()!!.compareTo(other.alpha()!!)
            ElideReleaseType.BETA -> beta()!!.compareTo(other.beta()!!)
            ElideReleaseType.RC -> releaseCandidate()!!.compareTo(other.releaseCandidate()!!)
            else -> error("Impossible: two stable versions are equal")
          }
          else -> typeCompare
        }
      }

      else -> semverCompare
    }
  }

  public fun satisfies(other: Semver): Boolean {
    val otherVersion = ElideVersionInfo(other.toString())
    val compared = compareTo(otherVersion)
    // must be equal or greater
    return compared >= 0
  }
}
