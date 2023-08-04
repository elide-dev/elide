/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.intrinsics.js.err

/**
 * # JavaScript: `ValueError`
 *
 * This type implements the API surface of a `ValueError` exception raised within the context of an executing JavaScript
 * guest. `ValueError` instances are typically raised when a value is passed to a function or operation that is not
 * valid or legal, although the type of the value is legal.
 *
 * An example of a `ValueError` would be the `port` property on a [elide.runtime.intrinsics.js.URL] object: if a value
 * is provided which is a valid [Int], but outside the range of valid port numbers (`1-65535`), a [ValueError] is raised
 * instead of a [TypeError].
 *
 * &nbsp;
 *
 * ## Further reading
 *
 * For more information about the expected behavior and API surface of a [ValueError], see the following resources:
 * - [MDN: `ValueError`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/ValueError)
 *
 * @see AbstractJSException for the host base interface type of all JavaScript exceptions.
 * @see Error for the top-most guest-exposed base class for all JavaScript errors.
 */
public abstract class ValueError : AbstractJSException, Error() {
  /** @inheritDoc */
  override val name: String get() = "ValueError"

  /**
   * ## Factory: `ValueError`
   *
   * Public factory for [ValueError] types. Java-style exceptions can be wrapped using the [create] method, or a string
   * message and cause can be provided, a-la Java exceptions.
   */
  public companion object Factory: AbstractJSException.ErrorFactory<ValueError> {
    override fun create(error: Throwable): ValueError {
      return object : ValueError() {
        override val message: String get() = error.message ?: "An error occurred"
      }
    }

    override fun create(message: String, cause: Throwable?): ValueError {
      return object : ValueError() {
        override val message: String get() = message
        override val cause: Error? get() = if (cause != null) {
          create(cause)
        } else {
          null
        }
      }
    }
  }
}
