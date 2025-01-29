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
package elide.runtime.intrinsics.js.err

import org.graalvm.polyglot.HostAccess.Implementable
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.PrintWriter
import java.io.StringWriter
import elide.vm.annotations.Polyglot

// Members and properties of a `TypeError`.
private val TYPE_ERROR_MEMBERS_AND_PROPS = arrayOf(
  "name",
  "message",
  "stack",
)

// @TODO: deprecate and replace with type mapping
// Installs `TypeError` into the environment.
//@Intrinsic @Singleton internal class TypeErrorIntrinsic : AbstractJsIntrinsic() {
//  override fun install(bindings: MutableIntrinsicBindings) {
//    bindings[TYPE_ERROR_SYMBOL.asJsSymbol()] = TypeError::class.java
//  }
//}

/**
 * # JavaScript: Type Error
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
 * @see AbstractJsException for the host base interface type of all JavaScript exceptions.
 * @see Error for the top-most guest-exposed base class for all JavaScript errors.
 */
public open class TypeError protected constructor (
  @get:Polyglot override val message: String,
  @get:Polyglot override val cause: Error?,
) : ProxyObject, AbstractJsException, Error() {

  // String-only constructor.
  public constructor(message: String?): this(message ?: "An error occurred", null)

  // Guest-value-only constructor.
  public constructor(value: Value?): this(value?.asString() ?: "An error occurred", null)

  // Empty constructor.
  public constructor(): this("An error occurred", null)

  @get:Polyglot override val name: String get() = "TypeError"
  override fun getMemberKeys(): Array<String> = TYPE_ERROR_MEMBERS_AND_PROPS
  override fun hasMember(key: String?): Boolean = key != null && key in TYPE_ERROR_MEMBERS_AND_PROPS
  override fun putMember(key: String?, value: Value?) { /* no-op */ }
  override fun removeMember(key: String?): Boolean = false

  override fun getMember(key: String?): Any? = when (key) {
    "name" -> name
    "message" -> message
    "stack" -> {
      // generate stacktrace
      val string = StringWriter()
      PrintWriter(string).use {
        printStackTrace()
      }
      string.toString()
    }

    else -> null
  }

  /**
   * ## Factory: `TypeError`
   *
   * Public factory for [TypeError] types. Java-style exceptions can be wrapped using the [create] method, or a string
   * message and cause can be provided, a-la Java exceptions.
   */
  public companion object Factory: AbstractJsException.ErrorFactory<TypeError>, ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any {
      return create(
        arguments.getOrNull(0)?.asString() ?: ""
      )
    }

    override fun create(error: Throwable): TypeError {
      return TypeError(error.message ?: "An error occurred")
    }

    override fun create(message: String, cause: Throwable?): TypeError {
      return TypeError(message, if (cause == null) null else object: Error() {
        override val message: String get() = cause.message ?: ""
        override val name: String get() = cause::class.java.simpleName
      })
    }
  }
}
