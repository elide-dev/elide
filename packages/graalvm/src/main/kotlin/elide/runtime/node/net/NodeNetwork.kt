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

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.NetAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable

// Installs the Node `net` module into the intrinsic bindings.
@Intrinsic internal class NodeNetworkModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeNetwork.create() }

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.NET)) { singleton }
  }
}

/**
 * # Node API: `net`
 */
internal class NodeNetwork : ReadOnlyProxyObject, NetAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeNetwork = NodeNetwork()
  }

  override fun getMemberKeys(): Array<String> = arrayOf("createServer")
  override fun getMember(key: String?): Any? = when (key) {
    "createServer" -> ProxyExecutable { _: Array<Value> ->
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("listen","close","on")
        override fun getMember(k: String?): Any? = when (k) {
          "listen" -> ProxyExecutable { argv: Array<Value> -> argv.lastOrNull()?.takeIf { it.canExecute() }?.execute(); this }
          "close" -> ProxyExecutable { _: Array<Value> -> this }
          "on" -> ProxyExecutable { _: Array<Value> -> this }
          else -> null
        }
      }
    }
    else -> null
  }
}
