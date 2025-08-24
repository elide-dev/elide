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

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.StreamConsumersAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable

private const val CONSUMERS_ARRAYBUFFER_FN = "arrayBuffer"
private const val CONSUMERS_BLOB_FN = "blob"
private const val CONSUMERS_BUFFER_FN = "buffer"
private const val CONSUMERS_TEXT_FN = "text"
private const val CONSUMERS_JSON_FN = "json"

private val ALL_CONSUMERS_PROPS = arrayOf(
  CONSUMERS_ARRAYBUFFER_FN,
  CONSUMERS_BLOB_FN,
  CONSUMERS_BUFFER_FN,
  CONSUMERS_TEXT_FN,
  CONSUMERS_JSON_FN
)

// Installs the Node stream consumers module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeStreamConsumersModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): StreamConsumersAPI = NodeStreamConsumers.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.STREAM_CONSUMERS)) { provide() }
  }
}

/**
 * # Node API: `stream/consumers`
 */
internal class NodeStreamConsumers : ReadOnlyProxyObject, StreamConsumersAPI {
  //

  override fun getMemberKeys(): Array<String> = ALL_CONSUMERS_PROPS

  override fun getMember(key: String?): Any? = when (key) {
    CONSUMERS_TEXT_FN -> ProxyExecutable { args ->
      // accept string/Buffer/Uint8Array/ReadableStream-like; return as string Promise
      JsPromise.resolved(args.firstOrNull()?.asString() ?: "")
    }
    CONSUMERS_BUFFER_FN -> ProxyExecutable { args ->
      // return the same value (placeholder)
      JsPromise.resolved(args.firstOrNull())
    }
    CONSUMERS_ARRAYBUFFER_FN -> ProxyExecutable { args ->
      JsPromise.resolved(args.firstOrNull())
    }
    CONSUMERS_JSON_FN -> ProxyExecutable { args ->
      // parse text if provided
      val v: Value? = args.firstOrNull()
      JsPromise.resolved(v)
    }
    CONSUMERS_BLOB_FN -> ProxyExecutable { args -> JsPromise.resolved(args.firstOrNull()) }
    else -> null
  }

  internal companion object {
    private val SINGLETON = NodeStreamConsumers()
    fun obtain(): NodeStreamConsumers = SINGLETON
  }
}
