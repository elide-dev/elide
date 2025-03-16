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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import jakarta.inject.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic

// Name of the `structuredClone` function in the global scope.
private const val CLONE_FN_NAME = "structuredClone"

// Public JavaScript symbol for the `structuredClone` function.
private val CLONE_FN_SYMBOL = CLONE_FN_NAME.asPublicJsSymbol()

/**
 * ## Structured Clone Built-in
 *
 * Implements the `structuredClone` global function for JavaScript, which is used to create a deep copy of an object,
 * usually for the purpose of transferring it between different execution contexts.
 *
 * ### Standards Compliance
 *
 * The Navigator `structuredClone` function is defined as part of the WinterTC Minimum Common API.
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Window/structuredClone)
 */
@Singleton
@Intrinsic(CLONE_FN_NAME) public class StructuredCloneBuiltin : ProxyExecutable, AbstractJsIntrinsic() {
  // language=JavaScript
  private val structuredCloner = """
    function doStructuredClone(value) {
        return JSON.parse(JSON.stringify(value));
    }
    doStructuredClone;
  """.trimIndent()

  private val cloner get() = Source.newBuilder("js", structuredCloner, "structuredCloner.js")
    .internal(true)
    .cached(true)
    .build()

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[CLONE_FN_SYMBOL] = this
  }

  public fun clone(value: Value, forContext: Context): Value {
    val cloner = forContext.eval(cloner)
    assert(cloner.canExecute()) { "Structured cloner function is not executable" }
    return requireNotNull(cloner.execute(value)) { "Failed to clone value" }
  }

  override fun execute(vararg arguments: Value?): Any? {
    val value = arguments.firstOrNull() ?: throw JsError.typeError("First argument to `structuredClone` is required")
    if (value.canExecute()) throw JsError.typeError("Cannot clone functions")
    return clone(value, value.context)
  }
}
