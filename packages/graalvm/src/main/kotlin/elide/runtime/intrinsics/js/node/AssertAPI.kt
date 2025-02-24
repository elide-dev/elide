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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Assert
 */
@API public interface AssertAPI : NodeAPI {
  /**
   * ## Assert: `ok(value, message)`
   *
   * Tests if `value` is truthy. It is equivalent to `assert.equal(!!value, true, message)`.
   *
   * If `value` is not truthy, an `AssertionError` is thrown with a message property set equal to the value of the
   * `message` parameter. If the `message` parameter is undefined, a default error message is assigned. If the `message`
   * parameter is an instance of an `Error` then it will be thrown as the error.
   *
   * @param values The first value is the value to test; the second, if any, is used as a string error message, thrown
   *   if the first value does not test as truthy.
   */
  @Polyglot public fun ok(vararg values: Any?)

  /**
   * ## Assert: `notOk(value, message)`
   *
   * Tests if `value` is falsy. It is equivalent to `assert.equal(!!value, false, message)`.
   *
   * If `value` is truthy, an `AssertionError` is thrown with a message property set equal to the value of the
   * `message` parameter. If the `message` parameter is undefined, a default error message is assigned. If the `message`
   * parameter is an instance of an `Error` then it will be thrown as the error.
   *
   * @param value The value to test.
   * @param message The message to display on error.
   */
  @Polyglot public fun notOk(value: Any?, message: Any? = null)

  /**
   * ## Assert: `fail(message)`
   */
  @Polyglot public fun fail(message: String? = null)

  /**
   * ## Assert: `fail(message)`
   */
  @Polyglot public fun fail(
    actual: Any? = null,
    expected: Any? = null,
    message: String? = null,
    operator: String? = null,
    stackStartFn: Any? = null
  )

  /**
   * ## Assert: `ifError(value)`
   */
  @Polyglot public fun ifError(value: Any? = null)

  /**
   * ## Assert: `ok(value, message)`
   */
  @Polyglot public fun assert(value: Any?, message: String? = null)

  /**
   * ## Assert: `equal(actual, expected, message)`
   */
  @Polyglot public fun equal(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `strict(actual, expected, message)`
   */
  @Polyglot public fun strict(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `notEqual(actual, expected, message)`
   */
  @Polyglot public fun notEqual(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `deepEqual(actual, expected, message)`
   */
  @Polyglot public fun deepEqual(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `notDeepEqual(actual, expected, message)`
   */
  @Polyglot public fun notDeepEqual(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `deepStrictEqual(actual, expected, message)`
   */
  @Polyglot public fun deepStrictEqual(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `notDeepStrictEqual(actual, expected, message)`
   */
  @Polyglot public fun notDeepStrictEqual(actual: Any?, expected: Any?, message: String? = null)

  /**
   * ## Assert: `match(string, regexp, message)`
   */
  @Polyglot public fun match(string: String, regexp: Regex, message: String? = null)

  /**
   * ## Assert: `match(string, regexp, message)`
   */
  @Polyglot public fun match(string: String, regexp: Value, message: String? = null)

  /**
   * ## Assert: `doesNotMatch(string, regexp, message)`
   */
  @Polyglot public fun doesNotMatch(string: String, regexp: Regex, message: String? = null)

  /**
   * ## Assert: `doesNotMatch(string, regexp, message)`
   */
  @Polyglot public fun doesNotMatch(string: String, regexp: Value, message: String? = null)

  /**
   * ## Assert: `throws(fn, error, message)`
   */
  @Polyglot public fun throws(fn: Any, error: Any? = null, message: String? = null)

  /**
   * ## Assert: `throws(error, message, fn)`
   */
  @Polyglot public fun throws(error: Any? = null, message: String? = null, fn: () -> Unit)

  /**
   * ## Assert: `doesNotThrow(fn, error, message)`
   */
  @Polyglot public fun doesNotThrow(fn: Any, error: Any? = null, message: String? = null)

  /**
   * ## Assert: `doesNotThrow(error, message, fn)`
   */
  @Polyglot public fun doesNotThrow(error: Any? = null, message: String? = null, fn: () -> Unit)

  /**
   * ## Assert: `rejects(asyncFn, error, message)`
   */
  @Polyglot public fun rejects(asyncFn: Any, error: Any? = null, message: String? = null)

  /**
   * ## Assert: `doesNotReject(asyncFn, error, message)`
   */
  @Polyglot public fun doesNotReject(asyncFn: Any, error: Any? = null, message: String? = null)
}
