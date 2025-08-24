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
  private class ReadOnlyTypeObject(private val name: String) : ReadOnlyProxyObject {
    override fun getMemberKeys(): Array<String> = emptyArray()
    override fun getMember(key: String?): Any? = null
    override fun toString(): String = "[object $name]"
  }

  internal companion object {
    @JvmStatic fun create(): NodeNetwork = NodeNetwork()
  }

  private val ALL_MEMBERS = arrayOf(
    "BlockList","SocketAddress","Server","Socket","connect","createConnection","createServer",
    "getDefaultAutoSelectFamily","getDefaultAutoSelectFamilyAttemptTimeout","isIP","isIPv4","isIPv6"
  )

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS
  override fun getMember(key: String?): Any? = when (key) {
    "BlockList" -> ReadOnlyTypeObject("BlockList")
    "SocketAddress" -> ReadOnlyTypeObject("SocketAddress")
    "Server" -> ReadOnlyTypeObject("Server")
    "Socket" -> ReadOnlyTypeObject("Socket")
    "connect","createConnection" -> ProxyExecutable { _: Array<Value> ->
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("end","destroy","on")
        override fun getMember(k: String?): Any? = when (k) {
          "end" -> ProxyExecutable { _: Array<Value> -> null }
          "destroy" -> ProxyExecutable { _: Array<Value> -> null }
          "on" -> ProxyExecutable { _: Array<Value> -> this }
          else -> null
        }
      }
    }
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
    "getDefaultAutoSelectFamily" -> ProxyExecutable { _: Array<Value> -> false }
    "getDefaultAutoSelectFamilyAttemptTimeout" -> ProxyExecutable { _: Array<Value> -> 0 }
    "isIP" -> ProxyExecutable { _: Array<Value> -> 0 }
    "isIPv4" -> ProxyExecutable { _: Array<Value> -> false }
    "isIPv6" -> ProxyExecutable { _: Array<Value> -> false }
    else -> null
  }
}
