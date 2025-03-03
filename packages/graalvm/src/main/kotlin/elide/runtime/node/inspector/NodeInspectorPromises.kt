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
package elide.runtime.node.inspector

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.InspectorPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName

// Symbol where the internal module implementation is installed.
private const val INSPECTOR_PROMISES_MODULE_SYMBOL: String = "node_${NodeModuleName.INSPECTOR_PROMISES}"

// Installs the Node inspector module into the intrinsic bindings.
@Intrinsic internal class NodeInspectorPromisesModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeInspectorPromises.create() }

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[INSPECTOR_PROMISES_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.INSPECTOR_PROMISES)) { singleton }
  }
}

internal class NodeInspectorPromises private constructor () : ReadOnlyProxyObject, InspectorPromisesAPI {
  companion object {
    @JvmStatic fun create(): NodeInspectorPromises = NodeInspectorPromises()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
