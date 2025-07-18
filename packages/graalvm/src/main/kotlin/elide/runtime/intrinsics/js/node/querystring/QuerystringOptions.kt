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
package elide.runtime.intrinsics.js.node.querystring

import org.graalvm.polyglot.Value

/**
 * ## Options: `querystring.parse`
 *
 * Describes the options which can be provided to a `parse` operation.
 *
 * @param maxKeys Maximum number of keys to parse; defaults to 1000.
 * @param decodeURIComponent Function to use when decoding percent-encoded characters; 
 *   defaults to querystring.unescape().
 */
public data class ParseOptions(
  // Should call NodeQueryString module's implementation of unescape(), but we can't access it here yet(?)
  val decodeURIComponent: Value? = null,
  val maxKeys: Int = DEFAULT_MAX_KEYS,
) {
  public companion object {
    /** Default maximum number of keys to parse. */
    public const val DEFAULT_MAX_KEYS: Int = 1000
    
    /** Default parse options. */
    public val DEFAULTS: ParseOptions = ParseOptions()

    /**
     * Convert a guest data structure to a [ParseOptions] structure on a best-effort basis.
     *
     * @param options The guest data structure to convert.
     * @return The converted [ParseOptions] structure.
     */
    @JvmStatic public fun fromGuest(options: Value?): ParseOptions = when {
      options == null || options.isNull -> DEFAULTS
      options.hasMembers() -> ParseOptions(
        maxKeys = options.getMember("maxKeys")?.takeIf { it.isNumber }?.asInt() ?: DEFAULT_MAX_KEYS,
        decodeURIComponent = options.getMember("decodeURIComponent")?.takeIf { it.canExecute() },
      )

      options.hasHashEntries() -> ParseOptions(
        maxKeys = options.getMember("maxKeys")?.takeIf { it.isNumber }?.asInt() ?: DEFAULT_MAX_KEYS,
        decodeURIComponent = options.getMember("decodeURIComponent")?.takeIf { it.canExecute() },
      )

      else -> throw IllegalArgumentException("Invalid options for querystring.parse: $options")
    }
  }
}

/**
 * ## Options: `querystring.stringify`
 *
 * Describes the options which can be provided to a `stringify` operation.
 *
 * @param encodeURIComponent Function to use when encoding characters; if null, it defaults to querystring.escape().
 */
public data class StringifyOptions(
  val encodeURIComponent: Value? = null,
) {
  public companion object {
    /** Default stringify options. */
    public val DEFAULTS: StringifyOptions = StringifyOptions()

    /**
     * Convert a guest data structure to a [StringifyOptions] structure on a best-effort basis.
     *
     * @param options The guest data structure to convert.
     * @return The converted [StringifyOptions] structure.
     */
    @JvmStatic public fun fromGuest(options: Value?): StringifyOptions = when {
      options == null || options.isNull -> DEFAULTS

      options.hasMembers() -> StringifyOptions(
        encodeURIComponent = options.getMember("encodeURIComponent")?.takeIf { it.canExecute() },
      )

      options.hasHashEntries() -> StringifyOptions(
        encodeURIComponent = options.getMember("encodeURIComponent")?.takeIf { it.canExecute() },
      )

      else -> throw IllegalArgumentException("Invalid options for querystring.stringify: $options")
    }
  }
}
