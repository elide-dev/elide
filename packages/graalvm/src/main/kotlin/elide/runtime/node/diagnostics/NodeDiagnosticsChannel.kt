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
package elide.runtime.node.diagnostics

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.DiagnosticsChannelAPI
import elide.runtime.lang.javascript.NodeModuleName

// Internal symbol where the Node built-in module is installed.
private const val DIAGNOSTICS_CHANNEL_MODULE_SYMBOL = "node_${NodeModuleName.DIAGNOSTICS_CHANNEL}"

// Installs the Node `diagnostics_channel` module into the intrinsic bindings.
@Intrinsic internal class NodeDiagnosticsChannelModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDiagnosticsChannel.create() }
  internal fun provide(): DiagnosticsChannelAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[DIAGNOSTICS_CHANNEL_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    // ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DIAGNOSTICS_CHANNEL)) { singleton }
  }
}

/**
 * # Node API: `diagnostics_channel`
 */
internal class NodeDiagnosticsChannel private constructor () : ReadOnlyProxyObject, DiagnosticsChannelAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeDiagnosticsChannel = NodeDiagnosticsChannel()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
