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
package elide.tooling.project.flags

import kotlinx.serialization.Serializable
import elide.annotations.API

/**
 * ## Project Flag Value
 *
 * Describes a value which is assigned to a given [ProjectFlag], and associated with a [ProjectFlagKey]; values must be
 * parsable from simple strings. See [ProjectFlag].
 *
 * @see ProjectFlag concept of project flags
 */
@Serializable
@API public sealed interface ProjectFlagValue {
  public val asString: String

  /**
   * ### Project Flag: No Value
   *
   * Sentinel which indicates that no value is set for a given flag.
   */
  @Serializable
  public data object NoValue : ProjectFlagValue {
    override val asString: String get() = ""
  }

  /**
   * ### Project Flag: Boolean Value
   *
   * Boolean-type value.
   *
   * @property value Raw value of this flag.
   */
  @Serializable
  public sealed interface BooleanTypeValue : ProjectFlagValue {
    public val value: Boolean
    override val asString: String get() = value.toString()
  }

  /**
   * ### Project Flag: True value.
   *
   * Sentinel which indicates a boolean `true` value.
   */
  @Serializable
  public data object True : BooleanTypeValue {
    override val value: Boolean get() = true
  }

  /**
   * ### Project Flag: False value.
   *
   * Sentinel which indicates a boolean `false` value.
   */
  @Serializable
  public data object False : BooleanTypeValue {
    override val value: Boolean get() = false
  }

  /**
   * ### Project Flag: String-type Value
   *
   * String-type value.
   *
   * @property value Raw value of this flag.
   */
  @Serializable
  public sealed interface StringTypeValue : ProjectFlagValue {
    public val value: String
    override val asString: String get() = value
  }

  /**
   * ### Project Flag: String-type Value
   *
   * String-type value.
   *
   * @property value Raw value of this flag.
   */
  @Serializable
  @JvmInline public value class StringValue (override val value: String) : StringTypeValue
}
