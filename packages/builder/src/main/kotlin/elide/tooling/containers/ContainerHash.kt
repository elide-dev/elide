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
 * ## Container Hash
 *
 * Represents a paired hash type and value, which uniquely identifies a container image or layer.
 */
@Serializable public sealed interface ContainerHash : ContainerComponent {
  /**
   * ### Hash Type
   *
   * The type of hash used to represent the container, such as SHA256.
   */
  public val hashType: ContainerHashType

  /**
   * ### Hash Value
   *
   * The actual hash value, which is a string representation of the hash.
   */
  public val hashValue: ContainerHashValue

  override fun asString(): String = "${hashType.asString()}:$hashValue"

  /**
   * ### Hash Pair
   *
   * Pairs a [ContainerHashType] with a [ContainerHashValue] to form a complete hash representation.
   */
  @Serializable @JvmInline public value class HashPair internal constructor (
    private val pair: Pair<ContainerHashType, ContainerHashValue>
  ) : ContainerHash {
    override val hashType: ContainerHashType get() = pair.first
    override val hashValue: ContainerHashValue get() = pair.second
  }

  /** No hash is present. */
  @Serializable public data object None: ContainerHash {
    override val hashType: ContainerHashType get() = ContainerHashType.SHA256
    override val hashValue: ContainerHashValue get() = ""
    override fun asString(): String = ""
  }

  /** Create and manipulate [ContainerHash] instances. */
  public companion object {
    /** @return Container hash with the provided [type] and [value]. */
    @JvmStatic public fun of(type: ContainerHashType, value: ContainerHashValue): ContainerHash = HashPair(
      type to value,
    )

    /** @return Empty container hash. */
    @JvmStatic public fun none(): ContainerHash = None
  }
}
