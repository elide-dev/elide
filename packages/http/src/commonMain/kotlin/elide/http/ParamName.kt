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

package elide.http

/**
 * ## HTTP Parameter Name
 */
public sealed interface ParamName : HttpToken {
  /**
   * ### Parameter Name
   *
   * Name of the parameter as a string.
   */
  public val name: String

  /**
   * Regular string parameter name.
   */
  @JvmInline public value class StringParamName internal constructor (override val name: String) : ParamName {
    override fun asString(): String = name
  }

  /** Factories for obtaining a [ParamName] instance. */
  public companion object {
    /** @return Wrapped [name] as a [ParamName]. */
    @JvmStatic public fun of(name: String): ParamName = StringParamName(name)
  }
}
