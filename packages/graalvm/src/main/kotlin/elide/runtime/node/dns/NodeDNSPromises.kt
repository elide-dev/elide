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
package elide.runtime.node.dns

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.DNSPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName

// Internal symbol where the Node built-in module is installed.
private const val DNS_PROMISES_MODULE_SYMBOL = "node_${NodeModuleName.DNS_PROMISES}"

// Installs the Node `dns/promises` module into the intrinsic bindings.
@Intrinsic internal class NodeDNSPromisesModule : AbstractNodeBuiltinModule() {
  internal fun provide(): DNSPromisesAPI = NodeDNSPromises.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[DNS_PROMISES_MODULE_SYMBOL.asJsSymbol()] = provide()
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DNS_PROMISES)) { provide() }
  }
}

/**
 * # Node API: `dns/promises`
 */
internal class NodeDNSPromises : ReadOnlyProxyObject, DNSPromisesAPI {
  //

  internal companion object {
    private val SINGLETON = NodeDNSPromises()
    fun obtain(): NodeDNSPromises = SINGLETON
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
