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
package elide.tooling.containers

import kotlinx.serialization.Serializable

/**
 * ## Container Coordinate
 *
 * Specifies a partial or complete container image reference/coordinate, which includes at least a container [name], and
 * may contain a [registry], [tag], and/or [hash].
 *
 * @see MutableContainerCoordinate mutable variant of this interface
 */
@Serializable public sealed interface ContainerCoordinate : ContainerComponent {
  /**
   * Registry component of the container's image coordinate.
   */
  public val registry: ContainerRegistry?

  /**
   * Container name component of the image coordinate.
   */
  public val name: ContainerName

  /**
   * Tag component of the container's image coordinate, if specified.
   */
  public val tag: ContainerTag?

  /**
   * Hash component of the container's image coordinate, if specified.
   */
  public val hash: ContainerHash?

  /**
   * ### Coordinate Info
   *
   * Carries immutable container coordinate information.
   */
  @JvmRecord @Serializable public data class CoordinateInfo internal constructor (
    override val name: ContainerName,
    override val registry: ContainerRegistry? = null,
    override val tag: ContainerTag? = null,
    override val hash: ContainerHash? = null,
  ) : ContainerCoordinate {
    override fun asString(): String = buildString {
      registry?.let {
        append(it.asString())
        append('/')
      }
      append(name)
      tag?.let {
        append(':')
        append(it.asString())
      }
      hash?.let {
        append('@')
        append(it.asString())
      }
    }
  }

  /** Utilities for creating and manipulating [ContainerCoordinate] instances. */
  public companion object {
    // Check a container name string for validity.
    private fun checkContainerName(name: ContainerName): ContainerName = name.also {
      require(name.isNotEmpty()) { "Container name cannot be empty" }
      require(name.isNotBlank()) { "Container name cannot be blank" }
    }

    /** @return Parsed [ContainerCoordinate] from the provided string [subject], or throws. */
    @JvmStatic public fun parse(subject: String): ContainerCoordinate {
      val hasHash = subject.contains('@')
      val hasTag = if (hasHash) {
        subject.substringBefore('@').contains(':')
      } else {
        subject.contains(':')
      }
      val registryAndRepo = subject.substringBefore(if (hasTag) ':' else '@')
      val containerName = registryAndRepo.substringAfterLast('/').substringBefore('@').substringBefore(':')
      val registry = if (registryAndRepo.contains('/')) {
        ContainerRegistry.of(subject.substringBeforeLast('/'))
      } else {
        null
      }
      val tag = if (hasTag) {
        ContainerTag.of(if (hasHash) {
          subject.removePrefix(registryAndRepo).substringBefore('@').substringAfterLast(':')
        } else {
          subject.substringAfterLast(':')
        })
      } else {
        null
      }
      val hash = if (hasHash) {
        val fullTag = subject.substringAfterLast('@')
        val hashType = fullTag.substringBefore(':')
        val hashValue = fullTag.substringAfter(':')
        ContainerHash.of(ContainerHashType.resolve(hashType), hashValue)
      } else {
        null
      }
      return CoordinateInfo(
        name = checkContainerName(containerName),
        registry = registry,
        tag = tag,
        hash = hash,
      )
    }

    /** @return Parsed [ContainerCoordinate] from the provided string [subject], or `null`. */
    @JvmStatic public fun parseSafe(subject: String): Result<ContainerCoordinate> = runCatching {
      parse(subject)
    }

    /** @return Well-formed container coordinate with the provided [name]. */
    @JvmStatic public fun of(name: ContainerName): ContainerCoordinate = CoordinateInfo(
      name = checkContainerName(name),
    )

    /** @return Well-formed container coordinate with the provided [name] and [registry]. */
    @JvmStatic public fun of(name: ContainerName, registry: ContainerRegistry): ContainerCoordinate = CoordinateInfo(
      name = checkContainerName(name),
      registry = registry,
    )

    /** @return Well-formed container coordinate with the provided [name], [registry], and [tag]. */
    @JvmStatic public fun of(name: ContainerName, registry: ContainerRegistry, tag: ContainerTag): ContainerCoordinate {
      return CoordinateInfo(
        name = checkContainerName(name),
        registry = registry,
        tag = tag,
      )
    }
  }
}
