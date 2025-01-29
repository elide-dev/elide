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
package elide.runtime.node.net

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.NetAPI

// Internal symbol where the Node built-in module is installed.
private const val NET_MODULE_SYMBOL = "node_net"

// Installs the Node `net` module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeNetworkModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): NetAPI = NodeNetwork.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[NET_MODULE_SYMBOL.asJsSymbol()] = provide()
  }
}

/**
 * # Node API: `net`
 */
internal class NodeNetwork : NetAPI {
  //

  internal companion object {
    private val SINGLETON = NodeNetwork()
    fun obtain(): NodeNetwork = SINGLETON
  }
}
