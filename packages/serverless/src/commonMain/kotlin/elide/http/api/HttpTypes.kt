/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.http.api

/**
 * ## HTTP: Text
 *
 * Describes the basic contract for text values which are carried over HTTP. Simply an alias to [CharSequence].
 */
public typealias HttpText = CharSequence

/**
 * ## HTTP: String
 *
 * Describes the basic contract for string values which are carried over HTTP. Simply an alias to [String].
 */
public typealias HttpString = String

/**
 * ## HTTP: Status Code
 *
 * Describes the basic contract provided for HTTP status code numerics, which are standardized by the various HTTP
 * specifications. HTTP status codes are never negative, so they are represented with [UInt] values.
 */
public typealias HttpStatusCode = UInt

/**
 * ## Case-insensitive HTTP String
 *
 * Describes an HTTP string which is case-insensitive for the purposes of basic expression and comparison, regardless of
 * input; the original value is preserved for later access, if needed.
 *
 * @param preserved The original HTTP string value.
 * @param normalized The normalized (lowercase) HTTP string value.
 */
public class CaseInsensitiveHttpString private constructor (
  private val preserved: HttpString,
  private val normalized: HttpString = preserved.trim().lowercase(),
): CharSequence by normalized {
  /** Static methods for creating case-insensitive HTTP strings. */
  public companion object {
    /**
     * Creates a new case-insensitive HTTP string from the given value.
     *
     * @param value The original HTTP string value.
     * @return A new case-insensitive HTTP string.
     */
    public fun of(value: HttpString): CaseInsensitiveHttpString = CaseInsensitiveHttpString(value)
  }

  /** Original preserved value for this case-insensitive string. */
  public val original: HttpString get() = preserved

  override fun toString(): HttpString = normalized
  override fun hashCode(): Int = normalized.hashCode()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return when (other) {
      is CaseInsensitiveHttpString -> normalized == other.normalized
      is String -> normalized.compareTo(other, ignoreCase = true) == 0
      else -> false
    }
  }
}
