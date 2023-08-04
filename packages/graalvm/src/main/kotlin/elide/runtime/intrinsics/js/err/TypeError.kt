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
 * # JavaScript: `TypeError`
 *
 * This type implements the API surface of a `TypeError` exception raised within the context of an executing JavaScript
 * guest. `TypeError` instances are typically raised when a value is passed to a function or operation that is not of
 * a legal or valid type.
 *
 * &nbsp;
 *
 * ## Further reading
 *
 * For more information about the expected behavior and API surface of a [TypeError], see the following resources:
 * - [MDN: `ValueError`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/TypeError)
 *
 * @see AbstractJSException for the host base interface type of all JavaScript exceptions.
 * @see Error for the top-most guest-exposed base class for all JavaScript errors.
 */
public abstract class TypeError : AbstractJSException, Error() {
  /** @inheritDoc */
  override val name: String get() = "TypeError"

  /**
   * ## Factory: `TypeError`
   *
   * Public factory for [TypeError] types. Java-style exceptions can be wrapped using the [create] method, or a string
   * message and cause can be provided, a-la Java exceptions.
   */
  public companion object Factory: AbstractJSException.ErrorFactory<TypeError> {
    /** @inheritDoc */
    override fun create(error: Throwable): TypeError {
      return object : TypeError() {
        override val message: String get() = error.message ?: "An error occurred"
      }
    }

    /** @inheritDoc */
    override fun create(message: String, cause: Throwable?): TypeError {
      return object : TypeError() {
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
