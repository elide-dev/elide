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

package elide.runtime.gvm.internals.intrinsics

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.LinkedList
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.api.ElideAPI
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.js.ElideJavaScriptLanguage
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.node.process.NodeProcess
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ProcessAPI
import elide.vm.annotations.Polyglot

// Symbol where the main Elide API is installed for guest use.
private const val ELIDE_SYMBOL = "Elide"

// Stubbed version of Elide.
private const val ELIDE_VERSION_STUBBED = "stubbed"

// Properties and methods made available for guest use.
private val BASE_ELIDE_PROPS_AND_METHODS = arrayOf(
  "process",
  "version",
)

public fun installElideBuiltin(name: String, value: Any) {
  ElideIntrinsic.install(name, value)
}

@Intrinsic(ELIDE_SYMBOL, internal = false) @Singleton internal class ElideIntrinsic :
  ElideAPI,
  ProxyObject,
  AbstractJsIntrinsic() {
  companion object {
    private val SINGLETON = ElideIntrinsic()

    internal fun install(name: String, value: Any) {
      SINGLETON.install(name, value)
    }
  }

  // All properties and methods for guest use, including deferred properties.
  private val deferredPropsAndMethods = LinkedList<String>()

  // Mapped property values for deferred properties
  private val mappedDeferredValue = HashMap<String, Any>()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[ELIDE_SYMBOL.asPublicJsSymbol()] = SINGLETON
  }

  internal fun install(name: String, value: Any) {
    deferredPropsAndMethods.add(name)
    mappedDeferredValue[name] = value
  }

  override fun supports(language: GuestLanguage): Boolean =
    language.engine == "js" || language.requestId == ElideJavaScriptLanguage.ID

  @get:Polyglot override val process: ProcessAPI get() = NodeProcess.obtain()
  @get:Polyglot override val version: String get() = ELIDE_VERSION_STUBBED

  override fun getMemberKeys(): Array<String> = BASE_ELIDE_PROPS_AND_METHODS + deferredPropsAndMethods.toTypedArray()
  override fun hasMember(key: String?): Boolean = key in BASE_ELIDE_PROPS_AND_METHODS || key in deferredPropsAndMethods

  override fun putMember(key: String?, value: Value?) {
    // no-op from guest code
  }

  override fun getMember(key: String?): Any? = when (key) {
    "process" -> process
    "version" -> version
    else -> mappedDeferredValue[key]
  }
}
