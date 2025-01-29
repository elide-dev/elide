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
package elide.runtime.intrinsics.js.node.stream

import java.nio.ByteBuffer

/**
 * TBD.
 */
internal interface ByteAccessor {
  /**
   * TBD.
   */
  val mutable: Boolean get() = false

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
  override val mutable: Boolean get() = false

  /**
   * TBD.
   */
  fun set(index: Int, value: Byte)

  companion object {
    /**
     * TBD.
     */
    @JvmStatic fun of(byteBuffer: ByteBuffer): MutableByteAccessor {
      return object : MutableByteAccessor {
        override fun size(): Int {
          return byteBuffer.capacity()
        }

        override fun get(index: Int): Byte {
          return byteBuffer.get(index)
        }

        override fun set(index: Int, value: Byte) {
          byteBuffer.put(index, value)
        }
      }
    }

    /**
     * TBD.
     */
    @JvmStatic fun of(byteArray: ByteArray): MutableByteAccessor {
      return object : MutableByteAccessor {
        override fun size(): Int {
          return byteArray.size
        }

        override fun get(index: Int): Byte {
          return byteArray[index]
        }

        override fun set(index: Int, value: Byte) {
          byteArray[index] = value
        }
      }
    }
  }
}
