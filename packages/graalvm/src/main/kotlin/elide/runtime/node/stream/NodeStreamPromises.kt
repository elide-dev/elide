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
package elide.runtime.node.stream

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.StreamPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.asJsSymbolString

// Internal symbol where the Node built-in module is installed.
private val STREAM_PROMISES_MODULE_SYMBOL = "node_${NodeModuleName.STREAM_PROMISES.asJsSymbolString()}"

// Constants for the stream promises module.
private const val PIPELINE_FN = "pipeline"
private const val FINISHED_FN = "finished"

// All module props.
private val ALL_PROMISES_PROPS = arrayOf(
  PIPELINE_FN,
  FINISHED_FN,
)

// Installs the Node stream promises module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeStreamPromisesModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): StreamPromisesAPI = NodeStreamPromises.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[STREAM_PROMISES_MODULE_SYMBOL.asJsSymbol()] = provide()
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.STREAM_PROMISES)) { provide() }
  }
}

/**
 * # Node API: `stream/promises`
 */
internal class NodeStreamPromises : ReadOnlyProxyObject, StreamPromisesAPI {
  //

  override fun getMemberKeys(): Array<String> = ALL_PROMISES_PROPS

  override fun getMember(key: String?): Any? = when (key) {
    PIPELINE_FN -> ProxyExecutable { TODO("`stream/promises.pipeline` is not implemented yet") }
    FINISHED_FN -> ProxyExecutable { TODO("`stream/promises.finished` is not implemented yet") }
    else -> null
  }

  internal companion object {
    private val SINGLETON = NodeStreamPromises()
    fun obtain(): NodeStreamPromises = SINGLETON
  }
}
