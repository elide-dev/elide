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
package elide.runtime.gvm.internals.node.inspector

import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.InspectorAPI

// Symbol where the internal module implementation is installed.
private const val INSPECTOR_MODULE_SYMBOL: String = "node_inspector"

// Installs the Node inspector module into the intrinsic bindings.
@Intrinsic internal class NodeInspectorModule : AbstractNodeBuiltinModule() {
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[INSPECTOR_MODULE_SYMBOL.asJsSymbol()] = NodeInspector.obtain()
  }
}

internal class NodeInspector : InspectorAPI {
  companion object {
    private val SINGLETON = NodeInspector()
    @JvmStatic fun obtain(): NodeInspector = SINGLETON
  }
}
