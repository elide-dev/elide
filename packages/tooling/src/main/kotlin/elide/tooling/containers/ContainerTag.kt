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
 * ## Container Tag
 *
 * Specifies the tag component for a container image reference.
 */
@Serializable public sealed interface ContainerTag : ContainerComponent {
  /**
   * ### Tag
   *
   * Value for a container's tag.
   */
  @Serializable @JvmInline public value class Tag internal constructor (
    public val value: ContainerTagValue,
  ): ContainerTag {
    override fun asString(): String = value
  }

  /** No tag present. */
  @Serializable public data object None : ContainerTag {
    override fun asString(): String = ""
  }

  /** Utilities for creating and manipulating [ContainerTag] instances. */
  public companion object {
    /** @return Container tag wrapping the provided [value]. */
    @JvmStatic public fun of(value: String): ContainerTag = Tag(value)

    /** @return No present container tag. */
    @JvmStatic public fun none(): ContainerTag = None
  }
}
