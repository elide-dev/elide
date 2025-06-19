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
 * ## Container Registry
 *
 * Describes the container image registry portion for a container's image reference.
 */
@Serializable public sealed interface ContainerRegistry : ContainerComponent {
  /**
   * Prefix to apply for the container registry.
   */
  public val prefix: ContainerRegistryValue

  override fun asString(): String = prefix

  /**
   * ## Container Registry Value
   *
   * Represents a value for a container registry, which may be a full URL or a simple name.
   */
  @Serializable @JvmInline public value class ContainerRegistryPrefix internal constructor (
    override val prefix: ContainerRegistryValue,
  ) : ContainerRegistry

  /** No present registry. */
  @Serializable public data object None : ContainerRegistry {
    override val prefix: ContainerRegistryValue get() = ""
    override fun asString(): String = ""
  }

  /** Utilities for creating and manipulating [ContainerRegistry] instances. */
  public companion object {
    /** @return Registry with the provided [prefix]. */
    @JvmStatic public fun of(prefix: String): ContainerRegistry = ContainerRegistryPrefix(prefix)

    /** @return No registry present. */
    @JvmStatic public fun none(prefix: String): ContainerRegistry = None
  }
}
