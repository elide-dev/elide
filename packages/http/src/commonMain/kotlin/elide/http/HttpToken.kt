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
 * Marker interface for tokens or token types which are meaningful within the HTTP protocol.
 */
public sealed interface HttpToken {
  /**
   * Format this token as a well-formed HTTP symbol.
   *
   * @return String symbol for this token.
   */
  public fun asString(): String

  /** HTTP token for a space. */
  public data object Space: HttpToken {
    override fun asString(): String = " "
  }

  /** HTTP token for a newline. */
  public data object Newline: HttpToken {
    override fun asString(): String = "\r"
  }

  /** HTTP token for a double newline. */
  public data object DoubleNewline: HttpToken {
    override fun asString(): String = "\r\n"
  }

  /** Simple string value. */
  @JvmInline public value class StringValue internal constructor(private val value: String) : HttpToken {
    override fun asString(): String = value
  }

  /** Factories for producing arbitrary string tokens. */
  public companion object {
    /** Simple string value as an HTTP token. */
    @JvmStatic public fun of(value: String): HttpToken = StringValue(value)
  }
}
