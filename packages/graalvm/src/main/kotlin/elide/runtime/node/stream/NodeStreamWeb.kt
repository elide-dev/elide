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
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.WebStreamsAPI

// Internal symbol where the Node built-in module is installed.
private const val STREAM_WEB_MODULE_SYMBOL = "node_stream_web"

// Installs the Node stream module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeStreamWebModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): WebStreamsAPI = NodeWebStreams.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[STREAM_WEB_MODULE_SYMBOL.asJsSymbol()] = provide()
    // @TODO cannot be synthetic because this module is satisfied by polyfills
    // ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.STREAM_WEB)) { provide() }
  }
}

// Web streams module constants.
private const val TRANSFORM_STREAM_CTOR = "TransformStream"
private const val TRANSFORM_STREAM_DEFAULT_CONTROLLER_CTOR = "TransformStreamDefaultController"
private const val QUEUING_STRATEGY_BYTELENGTH = "ByteLengthQueuingStrategy"
private const val QUEUING_STRATEGY_COUNT = "CountQueuingStrategy"
private const val TEXTENCODER_STREAM = "TextEncoderStream"
private const val TEXTDECODER_STREAM = "TextDecoderStream"
private const val COMPRESSION_STREAM = "CompressionStream"
private const val DECOMPRESSION_STREAM = "DecompressionStream"
private const val READABLE_STREAM = "ReadableStream"
private const val READABLE_STREAM_DEFAULT_READER = "ReadableStreamDefaultReader"
private const val READABLE_STREAM_BYOB_READER = "ReadableStreamBYOBReader"
private const val READABLE_STREAM_DEFAULT_CONTROLLER = "ReadableStreamDefaultController"
private const val READABLE_BYTE_STREAM_CONTROLLER = "ReadableByteStreamController"
private const val READABLE_STREAM_BYOB_REQUEST = "ReadableStreamBYOBRequest"
private const val WRITABLE_STREAM = "WritableStream"
private const val WRITABLE_STREAM_DEFAULT_WRITER = "WritableStreamDefaultWriter"
private const val WRITABLE_STREAM_DEFAULT_CONTROLLER = "WritableStreamDefaultController"
private const val TRANSFORM_STREAM = "TransformStream"
private const val TRANSFORM_STREAM_DEFAULT_CONTROLLER = "TransformStreamDefaultController"

// All module props.
private val WEB_STREAMS_PROPS = arrayOf(
  TRANSFORM_STREAM_CTOR,
  TRANSFORM_STREAM_DEFAULT_CONTROLLER_CTOR,
  QUEUING_STRATEGY_BYTELENGTH,
  QUEUING_STRATEGY_COUNT,
  TEXTENCODER_STREAM,
  TEXTDECODER_STREAM,
  COMPRESSION_STREAM,
  DECOMPRESSION_STREAM,
  READABLE_STREAM,
  READABLE_STREAM_DEFAULT_READER,
  READABLE_STREAM_BYOB_READER,
  READABLE_STREAM_DEFAULT_CONTROLLER,
  READABLE_BYTE_STREAM_CONTROLLER,
  READABLE_STREAM_BYOB_REQUEST,
  WRITABLE_STREAM,
  WRITABLE_STREAM_DEFAULT_WRITER,
  WRITABLE_STREAM_DEFAULT_CONTROLLER,
  TRANSFORM_STREAM,
  TRANSFORM_STREAM_DEFAULT_CONTROLLER
)

/**
 * # Node API: `stream/web`
 */
internal class NodeWebStreams : ReadOnlyProxyObject, WebStreamsAPI {
  //

  override fun getMemberKeys(): Array<String> = WEB_STREAMS_PROPS

  override fun getMember(key: String?): Any? = null

  internal companion object {
    private val SINGLETON = NodeWebStreams()
    fun obtain(): NodeWebStreams = SINGLETON
  }
}
