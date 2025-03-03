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
package elide.runtime.node.asserts

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.AssertStrictAPI
import elide.runtime.lang.javascript.NodeModuleName

// Symbol where the internal module implementation is installed.
private const val ASSERT_STRICT_MODULE_SYMBOL: String = "node_${NodeModuleName.ASSERT_STRICT}"

// Installs the Node assert module into the intrinsic bindings.
@Intrinsic internal class NodeAssertStrictModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeAssertStrict.create() }

  // Provide a compliant instance of the OS API to the DI context.
  fun provide(): AssertStrictAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[ASSERT_STRICT_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.ASSERT_STRICT)) { singleton }
  }
}

/**
 * # Node API: `assert/strict`
 */
internal class NodeAssertStrict private constructor() : ReadOnlyProxyObject, AssertStrictAPI {
  //

  internal companion object {
    fun create(): NodeAssertStrict = NodeAssertStrict()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
