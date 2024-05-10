/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.gvm.internals.node.buffer

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import java.nio.ByteBuffer
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.stream.ByteAccessor
import elide.runtime.intrinsics.js.node.stream.MutableByteAccessor

// Symbol at which the main `Buffer` intrinsic is installed.
private const val BUFFER_SYMBOL = "Buffer"

// Internal symbol where the Node built-in module is installed.
private const val BUFFER_MODULE_SYMBOL = "__Elide_node_buffer__"

// Default setting for direct buffers.
private const val DEFAULT_DIRECT_BUFFERS = false

/**
 * TBD.
 */
internal interface NodeBufferFactory<T> where T: NodeDataBuffer {
  /**
   *
   */
  fun create(arguments: Array<Value?>): T

  /**
   *
   */
  fun alloc(size: UInt = 0u, direct: Boolean? = null): T

  /**
   *
   */
  fun of(accessor: ByteAccessor): T

  /**
   *
   */
  fun of(byteBuffer: ByteBuffer): T

  /**
   *
   */
  fun of(byteBuffer: ByteArray): T

  /**
   *
   */
  fun empty(): T

  /**
   *
   */
  fun ofMutable(byteBuffer: ByteBuffer): T

  /**
   *
   */
  fun ofMutable(byteBuffer: ByteArray): T

  companion object {
    // Resolve a factory for the Node `Buffer` intrinsic.
    @JvmStatic fun factory(direct: Boolean? = null): NodeBufferFactory<out NodeDataBuffer> =
      if (direct ?: DEFAULT_DIRECT_BUFFERS) NodeDirectDataBuffer else NodeHeapDataBuffer
  }
}

// Installs the Node `buffer` built-in module.
@Intrinsic internal class NodeBufferModule : AbstractNodeBuiltinModule() {
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[BUFFER_SYMBOL.asJsSymbol()] = ProxyInstantiable {
      NodeBufferFactory.factory().create(it)
    }
  }
}

// Installs the Node `Buffer` built-in intrinsic.
@Intrinsic internal class NodeBufferIntrinsic : AbstractJsIntrinsic() {
  @Inject lateinit var facade: NodeBufferModuleFacade

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[BUFFER_MODULE_SYMBOL.asJsSymbol()] = facade
  }
}

// Module facade which satisfies the built-in `Buffer` module.
@Singleton internal class NodeBufferModuleFacade : BufferAPI {
  // TBD.
}

/**
 * TBD.
 */
internal sealed class NodeDataBuffer (protected val accessor: ByteAccessor) : Buffer {
  override val length: Int by lazy { accessor.size() }

  override fun get(index: Long): Byte = accessor.get(index.toInt())

  override fun set(index: Long, value: Value?) {
    if (accessor !is MutableByteAccessor) {
      throw UnsupportedOperationException("Buffer is not mutable")
    }
    if (value == null)
      throw IllegalArgumentException("Value cannot be null")

    accessor.set(index.toInt(), value.asByte())
  }

  override fun getSize(): Long = length.toLong()

  override fun getMember(key: String): Any = when (key) {
    "length" -> length
    else -> throw IllegalArgumentException("Unknown member key: $key")
  }
}

/**
 * TBD.
 */
internal class NodeDirectDataBuffer private constructor (accessor: ByteAccessor) : NodeDataBuffer(accessor) {
  override fun newInstance(vararg arguments: Value?): Any {
    TODO("Not yet implemented")
  }

  companion object : NodeBufferFactory<NodeDirectDataBuffer> {
    private val EMPTY_BUFFER_SINGLETON = NodeDirectDataBuffer(ByteAccessor.of(ByteArray(0)))

    override fun of(accessor: ByteAccessor): NodeDirectDataBuffer = NodeDirectDataBuffer(accessor)
    override fun of(byteBuffer: ByteBuffer): NodeDirectDataBuffer = NodeDirectDataBuffer(ByteAccessor.of(byteBuffer))
    override fun of(byteBuffer: ByteArray): NodeDirectDataBuffer = NodeDirectDataBuffer(ByteAccessor.of(byteBuffer))
    override fun empty(): NodeDirectDataBuffer = EMPTY_BUFFER_SINGLETON

    override fun ofMutable(byteBuffer: ByteBuffer): NodeDirectDataBuffer =
      NodeDirectDataBuffer(MutableByteAccessor.of(byteBuffer))

    override fun ofMutable(byteBuffer: ByteArray): NodeDirectDataBuffer =
      NodeDirectDataBuffer(MutableByteAccessor.of(byteBuffer))

    override fun create(arguments: Array<Value?>): NodeDirectDataBuffer {
      TODO("Not yet implemented: `NodeDirectDataBuffer.create()`")
    }

    override fun alloc(size: UInt, direct: Boolean?): NodeDirectDataBuffer {
      return if (size == 0u) {
        empty()
      } else {
        TODO("Not yet implemented")
      }
    }
  }
}

/**
 * TBD.
 */
internal class NodeHeapDataBuffer private constructor (accessor: ByteAccessor) : NodeDataBuffer(accessor) {
  override fun newInstance(vararg arguments: Value?): Any {
    TODO("Not yet implemented")
  }

  companion object : NodeBufferFactory<NodeHeapDataBuffer> {
    private val EMPTY_BUFFER_SINGLETON = NodeHeapDataBuffer(ByteAccessor.of(ByteArray(0)))

    override fun of(accessor: ByteAccessor): NodeHeapDataBuffer = NodeHeapDataBuffer(accessor)
    override fun of(byteBuffer: ByteBuffer): NodeHeapDataBuffer = NodeHeapDataBuffer(ByteAccessor.of(byteBuffer))
    override fun of(byteBuffer: ByteArray): NodeHeapDataBuffer = NodeHeapDataBuffer(ByteAccessor.of(byteBuffer))
    override fun empty(): NodeHeapDataBuffer = EMPTY_BUFFER_SINGLETON

    override fun ofMutable(byteBuffer: ByteBuffer): NodeHeapDataBuffer =
      NodeHeapDataBuffer(MutableByteAccessor.of(byteBuffer))

    override fun ofMutable(byteBuffer: ByteArray): NodeHeapDataBuffer =
      NodeHeapDataBuffer(MutableByteAccessor.of(byteBuffer))

    override fun create(arguments: Array<Value?>): NodeHeapDataBuffer {
      TODO("Not yet implemented: `NodeHeapDataBuffer.create()`")
    }

    override fun alloc(size: UInt, direct: Boolean?): NodeHeapDataBuffer {
      TODO("Not yet implemented")
    }
  }
}
