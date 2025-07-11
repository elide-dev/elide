/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # Node API: `querystring`
 *
 * Describes the API provided by the Node API built-in `querystring` module, which provides URL query string parsing
 * and serialization functionality. It handles URL percent-encoding and decoding, as well as conversion between
 * query strings and JavaScript objects.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * This module provides URL query string parsing and serialization functionality.
 *
 * The core functions include:
 *
 * - `parse()` - Parse query strings into JavaScript objects
 * - `stringify()` - Serialize JavaScript objects into query strings
 * - `escape()` - URL percent-encode strings
 * - `unescape()` - URL percent-decode strings
 *
 * **Aliases:**
 * - `decode()` - Alias for `parse()`
 * - `encode()` - Alias for `stringify()`
 */
@API public interface QuerystringAPI : NodeAPI {
  /**
   * ## Method: `querystring.decode(str, sep, eq, options)`
   *
   * Alias for `querystring.parse()`. Parses a URL query string into a JavaScript object.
   *
   * @param str The URL query string to parse.
   * @param sep The substring used to delimit key-value pairs (default: '&').
   * @param eq The substring used to delimit keys and values (default: '=').
   * @param options Optional parsing configuration.
   * @return A JavaScript object representing the parsed query string.
   */
  @Polyglot public fun decode(
    str: Value,
    sep: Value? = Value.asValue("&"),
    eq: Value? = Value.asValue("="),
    options: Value? = null
  ): Value

  /**
   * ## Method: `querystring.encode(obj, sep, eq, options)`
   *
   * Alias for `querystring.stringify()`. Serializes a JavaScript object into a URL query string.
   *
   * @param obj The object to serialize.
   * @param sep The substring used to delimit key-value pairs (default: '&').
   * @param eq The substring used to delimit keys and values (default: '=').
   * @param options Optional serialization configuration.
   * @return A URL query string representing the serialized object.
   */
  @Polyglot public fun encode(
    obj: Value,
    sep: Value? = Value.asValue("&"),
    eq: Value? = Value.asValue("="),
    options: Value? = null
  ): String

  /**
   * ## Method: `querystring.escape(str)`
   *
   * Performs URL percent-encoding on the given string in a manner that is optimized for
   * the specific requirements of URL query strings.
   *
   * @param str The string to percent-encode.
   * @return The percent-encoded string.
   */
  @Polyglot public fun escape(str: Value): String

  /**
   * ## Method: `querystring.parse(str, sep, eq, options)`
   *
   * Parses a URL query string into a JavaScript object. The returned object does not
   * prototype inherit from the JavaScript Object. This means that typical Object methods
   * such as `obj.toString()`, `obj.hasOwnProperty()`, and others are not defined and will not work.
   *
   * @param str The URL query string to parse.
   * @param sep The substring used to delimit key-value pairs (default: '&').
   * @param eq The substring used to delimit keys and values (default: '=').
   * @param options Optional parsing configuration.
   * @return A JavaScript object representing the parsed query string.
   */
  @Polyglot public fun parse(
    str: Value,
    sep: Value? = Value.asValue("&"),
    eq: Value? = Value.asValue("="),
    options: Value? = null
  ): Value

  /**
   * ## Method: `querystring.stringify(obj, sep, eq, options)`
   *
   * Serializes a JavaScript object into a URL query string by iterating through the object's
   * "own properties". It serializes the following types of values passed in obj: string,
   * number, boolean, string[], number[], boolean[].
   *
   * @param obj The object to serialize.
   * @param sep The substring used to delimit key-value pairs (default: '&').
   * @param eq The substring used to delimit keys and values (default: '=').
   * @param options Optional serialization configuration.
   * @return A URL query string representing the serialized object.
   */
  @Polyglot public fun stringify(
    obj: Value,
    sep: Value? = Value.asValue("&"),
    eq: Value? = Value.asValue("="),
    options: Value? = null
  ): String

  /**
   * ## Method: `querystring.unescape(str)`
   *
   * Performs decoding of URL percent-encoded characters on the given string.
   *
   * @param str The string to decode.
   * @return The decoded string.
   */
  @Polyglot public fun unescape(str: Value): String
}
