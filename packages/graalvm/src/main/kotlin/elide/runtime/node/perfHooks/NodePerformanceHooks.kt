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
package elide.runtime.node.perfHooks

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.PerformanceHooksAPI
import elide.runtime.lang.javascript.NodeModuleName

// Internal symbol where the Node built-in module is installed.
private const val PERFORMANCE_HOOKS_MODULE_SYMBOL = "node_${NodeModuleName.PERF_HOOKS}"

// Installs the Node performance hooks module into the intrinsic bindings.
@Intrinsic internal class NodePerformanceHooksModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodePerformanceHooks.create() }

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[PERFORMANCE_HOOKS_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.PERF_HOOKS)) { singleton }
  }
}

/**
 * # Node API: `perf_hooks`
 */
internal class NodePerformanceHooks private constructor () : ReadOnlyProxyObject, PerformanceHooksAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodePerformanceHooks = NodePerformanceHooks()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
