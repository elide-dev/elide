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
package elide.runtime.intrinsics.js.err

import org.graalvm.polyglot.HostAccess.Implementable
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.PrintWriter
import java.io.StringWriter
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.vm.annotations.Polyglot

// Internal symbol for a `ValueError`.
private const val VALUE_ERROR_SYMBOL = "ValueError"

// Members and properties of a `ValueError`.
private val VALUE_ERROR_MEMBERS_AND_PROPS = arrayOf(
  "name",
  "message",
  "stack",
)

// Installs `ValueError` into the environment.
@Intrinsic(VALUE_ERROR_SYMBOL, internal = false)
@Singleton internal class ValueErrorIntrinsic : AbstractJsIntrinsic() {
  @OptIn(DelicateElideApi::class)
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[VALUE_ERROR_SYMBOL.asPublicJsSymbol()] = ValueError::class.java
  }
}

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
 * @see AbstractJsException for the host base interface type of all JavaScript exceptions.
 * @see Error for the top-most guest-exposed base class for all JavaScript errors.
 */
@Implementable
public open class ValueError protected constructor(
  override val message: String,
  override val cause: Error? = null,
) : AbstractJsException, ProxyObject, Error() {

  // Guest-value-only constructor.
  @Polyglot public constructor(value: Value?): this(value?.asString() ?: "An error occurred", null)

  // Empty constructor.
  @Polyglot public constructor(): this("An error occurred", null)

  override val name: String get() = "NativeValueError"
  override fun getMemberKeys(): Array<String> = VALUE_ERROR_MEMBERS_AND_PROPS
  override fun hasMember(key: String?): Boolean = key != null && key in VALUE_ERROR_MEMBERS_AND_PROPS
  override fun putMember(key: String?, value: Value?) { /* no-op */ }
  override fun removeMember(key: String?): Boolean = false

  override fun getMember(key: String?): Any? = when (key) {
    "name" -> name
    "message" -> message
    "stack" -> {
      val sw = StringWriter()
      PrintWriter(sw).use {
        printStackTrace()
      }
      sw.toString()
    }
    else -> null
  }

  /**
   * ## Factory: `ValueError`
   *
   * Public factory for [ValueError] types. Java-style exceptions can be wrapped using the [create] method, or a string
   * message and cause can be provided, a-la Java exceptions.
   */
  public companion object Factory: AbstractJsException.ErrorFactory<ValueError>, ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any {
      return create(arguments[0]?.asString() ?: "An error occurred")
    }

    override fun create(error: Throwable): ValueError {
      return ValueError(error.message ?: "An error occurred", object : Error() {
        override val message: String get() = error.message ?: ""
        override val name: String get() = error::class.java.simpleName
      })
    }

    override fun create(message: String, cause: Throwable?): ValueError {
      return ValueError(message, if (cause == null) null else object: Error() {
        override val message: String get() = cause.message ?: ""
        override val name: String get() = cause::class.java.simpleName
      })
    }
  }
}
