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
package elide.runtime.node.dgram


import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.DatagramAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

// Internal symbol where the Node built-in module is installed.
private const val DATAGRAM_MODULE_SYMBOL = "node_${NodeModuleName.DGRAM}"

// Installs the Node dgram module into the intrinsic bindings.
@Intrinsic internal class NodeDatagramModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDatagram.create() }
  internal fun provide(): DatagramAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[DATAGRAM_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DGRAM)) { singleton }
  }
}

/**
 * # Node API: `datagram`
 */
internal class NodeDatagram private constructor () : ReadOnlyProxyObject, DatagramAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeDatagram = NodeDatagram()
  }

  override fun getMemberKeys(): Array<String> = arrayOf("createSocket")
  override fun getMember(key: String?): Any? = when (key) {
    "createSocket" -> ProxyExecutable { _: Array<Value> ->
      // minimal UDP socket facade with bind/send/close
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("bind","send","close","on")
        override fun getMember(k: String?): Any? = when (k) {
          "bind" -> ProxyExecutable { argv: Array<Value> -> argv.lastOrNull()?.takeIf { it.canExecute() }?.execute(); this }
          "send" -> ProxyExecutable { argv: Array<Value> ->
            // send(buf, offset, length, port, address, cb)
            argv.lastOrNull()?.takeIf { it.canExecute() }?.execute()
            0
          }
          "close" -> ProxyExecutable { _: Array<Value> -> null }
          "on" -> ProxyExecutable { _: Array<Value> -> this }
          else -> null
        }
      }
    }
    else -> null
  }
}
