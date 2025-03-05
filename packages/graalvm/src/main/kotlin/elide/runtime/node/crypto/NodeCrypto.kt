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
package elide.runtime.node.crypto

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.CryptoAPI
import elide.runtime.lang.javascript.NodeModuleName

// Internal symbol where the Node built-in module is installed.
private const val CRYPTO_MODULE_SYMBOL = "node_${NodeModuleName.CRYPTO}"

// Installs the Node crypto module into the intrinsic bindings.
@Intrinsic internal class NodeCryptoModule : AbstractNodeBuiltinModule() {
  internal fun provide(): NodeCrypto = NodeCrypto.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[CRYPTO_MODULE_SYMBOL.asJsSymbol()] = provide()
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.CRYPTO)) { provide() }
  }
}

/**
 * # Node API: `crypto`
 */
internal class NodeCrypto : ReadOnlyProxyObject, CryptoAPI {
  //

  internal companion object {
    private val SINGLETON = NodeCrypto()
    fun obtain(): NodeCrypto = SINGLETON
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
