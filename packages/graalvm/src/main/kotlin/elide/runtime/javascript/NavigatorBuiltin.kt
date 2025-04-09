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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.javascript

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import jakarta.inject.Singleton
import kotlinx.atomicfu.atomic
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.ElideIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic

// Name of the `navigator` object in the global scope.
private const val NAVIGATOR_NAME = "navigator"

// Name of the `userAgent` property on the `navigator` object.
private const val NAVIGATOR_UA_NAME = "userAgent"

// Public JavaScript symbol for the `navigator` object.
private val NAVIGATOR_SYMBOL = NAVIGATOR_NAME.asPublicJsSymbol()

// Name of the `Navigator` class in the global scope.
private const val NAVIGATOR_CLASS_NAME = "Navigator"

// Public JavaScript symbol for the `Navigator` class.
private val NAVIGATOR_CLASS_SYMBOL = NAVIGATOR_CLASS_NAME.asPublicJsSymbol()

// User-Agent token to include in the `navigator.userAgent` property.
private const val ELIDE_UA_TOKEN = "Elide"

/**
 * ## Navigator Built-in
 *
 * Built-in which provides the `navigator` object in the global JavaScript scope; this object mimics a subset of what is
 * available in browsers.
 *
 * ### Standards Compliance
 *
 * The Navigator `userAgent` property is defined as part of the WinterTC Minimum Common API.
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Window/navigator)
 */
@Singleton
@Intrinsic(NAVIGATOR_NAME) public class NavigatorBuiltin : ReadOnlyProxyObject, AbstractJsIntrinsic() {
  public class Navigator : ProxyInstantiable {
    override fun newInstance(vararg arguments: Value?): Any? = throw JsError.typeError(
      "ERR_ILLEGAL_CONSTRUCTOR",
      "Illegal constructor",
    )
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[NAVIGATOR_SYMBOL] = this
    bindings[NAVIGATOR_CLASS_SYMBOL] = Navigator()
  }

  override fun getMemberKeys(): Array<String> = arrayOf(NAVIGATOR_UA_NAME)

  override fun getMember(key: String): Any? = when (key) {
    NAVIGATOR_UA_NAME -> elideUserAgent()
    else -> null
  }

  private fun elideUserAgent(): String = "$ELIDE_UA_TOKEN/${ElideIntrinsic.version()}"
}
