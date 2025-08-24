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
import elide.runtime.gvm.internals.intrinsics.js.codec.TextDecoder
import elide.runtime.gvm.internals.intrinsics.js.codec.TextEncoder
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.StreamConsumersAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

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
      // Accept Buffer | Uint8Array | ArrayBuffer | string; resolve to string
      val v: Value? = args.firstOrNull()
      val bytes: ByteArray? = when {
        v == null || v.isNull -> ByteArray(0)
        v.isString -> TextEncoder().encode(v.asString())
        v.hasArrayElements() -> {
          // Read as Uint8Array/Buffer/ArrayBuffer
          val len = v.arraySize.toInt()
          val out = ByteArray(len)
          var i = 0
          while (i < len) { out[i] = (v.getArrayElement(i.toLong()).asInt() and 0xFF).toByte(); i++ }
          out
        }
        else -> ByteArray(0)
      }
      JsPromise.resolved(TextDecoder().decode(Value.asValue(bytes)))
    }
    CONSUMERS_BUFFER_FN -> ProxyExecutable { args ->
      // Resolve to a Node Buffer-like (return original if it looks like a Buffer/Uint8Array)
      val v: Value? = args.firstOrNull()
      JsPromise.resolved(v)
    }
    CONSUMERS_ARRAYBUFFER_FN -> ProxyExecutable { args ->
      val v: Value? = args.firstOrNull()
      // If we have a Buffer/Uint8Array, return its underlying ArrayBuffer; otherwise pass-through
      val ab = v?.getMember("buffer") ?: v
      JsPromise.resolved(ab)
    }
    CONSUMERS_JSON_FN -> ProxyExecutable { args ->
      // Parse as JSON if string-like, else pass-through
      val v: Value? = args.firstOrNull()
      val ctx = Context.getCurrent()
      val JSON = ctx.getBindings("js").getMember("JSON")
      val parse = JSON.getMember("parse")
      val text: String = when {
        v == null || v.isNull -> "null"
        v.isString -> v.asString()
        v.hasArrayElements() -> TextDecoder().decode(Value.asValue(ByteArray(v.arraySize.toInt()) { i ->
          (v.getArrayElement(i.toLong()).asInt() and 0xFF).toByte()
        }))
        else -> "null"
      }
      JsPromise.resolved(parse.execute(text))
    }
    CONSUMERS_BLOB_FN -> ProxyExecutable { args ->
      val v: Value? = args.firstOrNull()
      // Construct a minimal Blob via global constructor if available
      val bindings = Context.getCurrent().getBindings("js")
      val blobCtor = bindings.getMember("Blob")
      val array = bindings.getMember("Array")
      val arr = array.newInstance()
      arr.setArrayElement(0, v)
      JsPromise.resolved(blobCtor.execute(arr))
    }
    else -> null
  }

  internal companion object {
    private val SINGLETON = NodeStreamConsumers()
    fun obtain(): NodeStreamConsumers = SINGLETON
  }
}
