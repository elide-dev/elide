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
package elide.runtime.node.http2

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.HTTP2API
import elide.runtime.lang.javascript.NodeModuleName

// Internal symbol where the Node built-in module is installed.
private const val HTTP2_MODULE_SYMBOL = "node_${NodeModuleName.HTTP2}"

// Installs the Node `http2` module into the intrinsic bindings.
@Intrinsic internal class NodeHttp2Module : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp2.create() }
  internal fun provide(): HTTP2API = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[HTTP2_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { provide() }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP2)) { provide() }
  }
}

/**
 * # Node API: `http2`
 */
internal class NodeHttp2 private constructor () : ReadOnlyProxyObject, HTTP2API {
  //

  internal companion object {
    @JvmStatic fun create(): NodeHttp2 = NodeHttp2()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
