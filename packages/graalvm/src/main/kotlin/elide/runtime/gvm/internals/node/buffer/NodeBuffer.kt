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
import java.nio.ByteBuffer
import elide.runtime.intrinsics.js.node.buffer.Buffer

/**
 * TBD.
 */
internal interface ByteAccessor {
  /**
   * TBD.
   */
  fun size(): Int

  /**
   * TBD.
   */
  fun get(index: Int): Byte

  companion object {
    /**
     * TBD.
     */
    @JvmStatic fun of(byteBuffer: ByteBuffer): ByteAccessor {
      return object : ByteAccessor {
        override fun size(): Int {
          return byteBuffer.capacity()
        }

        override fun get(index: Int): Byte {
          return byteBuffer.get(index)
        }
      }
    }

    /**
     * TBD.
     */
    @JvmStatic fun of(byteArray: ByteArray): ByteAccessor {
      return object : ByteAccessor {
        override fun size(): Int {
          return byteArray.size
        }

        override fun get(index: Int): Byte {
          return byteArray[index]
        }
      }
    }
  }
}

/**
 * TBD.
 */
internal interface MutableByteAccessor : ByteAccessor {
  /**
   * TBD.
   */
  fun set(index: Int, value: Byte)

  companion object {
    /**
     * TBD.
     */
    @JvmStatic fun of(byteBuffer: ByteBuffer): MutableByteAccessor {
      TODO("Mutable byte accessors are not supported yet.")
    }

    /**
     * TBD.
     */
    @JvmStatic fun of(byteArray: ByteArray): MutableByteAccessor {
      TODO("Mutable byte accessors are not supported yet.")
    }
  }
}

/**
 * TBD.
 */
internal interface NodeBufferFactory {

}

/**
 * TBD.
 */
internal sealed class NodeDataBuffer (protected val accessor: ByteAccessor) : Buffer {
  override val length: Int by lazy { accessor.size() }

  override fun get(index: Long): Any {
    TODO("Not yet implemented")
  }

  override fun set(index: Long, value: Value?) {
    TODO("Not yet implemented")
  }

  override fun getSize(): Long {
    TODO("Not yet implemented")
  }
}

/**
 * TBD.
 */
internal class NodeDirectDataBuffer private constructor (accessor: ByteAccessor) : NodeDataBuffer(accessor) {
  override fun newInstance(vararg arguments: Value?): Any {
    TODO("Not yet implemented")
  }

  companion object : NodeBufferFactory {

  }
}

/**
 * TBD.
 */
internal class NodeHeapDataBuffer private constructor (accessor: ByteAccessor) : NodeDataBuffer(accessor) {
  override fun newInstance(vararg arguments: Value?): Any {
    TODO("Not yet implemented")
  }

  companion object : NodeBufferFactory {

  }
}
