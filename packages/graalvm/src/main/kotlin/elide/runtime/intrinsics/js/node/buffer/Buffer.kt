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
package elide.runtime.intrinsics.js.node.buffer

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyObject
import java.nio.ByteBuffer
import elide.runtime.gvm.internals.node.buffer.NodeBufferFactory
import elide.vm.annotations.Polyglot

// Keys on `Buffer` objects.
private val BUFFER_KEYS = arrayOf(
  "length",
  "byteLength",
  "compare",
  "concat",
  "copy",
  "isBuffer",
  "isEncoding",
  "poolSize",
)

/**
 * # Node: Buffer
 */
public interface Buffer : java.io.Serializable, ProxyObject, ProxyArray, ProxyInstantiable, ProxyIterable {
  /**
   * ## Buffer: Constructors
   */
  public interface BufferConstructors {
    /**
     * TBD.
     */
    @Polyglot public fun create(array: Value, encoding: String? = null): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun create(array: Value, byteOffset: Int? = null, length: Int? = null): Buffer
  }

  /**
   * ## Buffer: Static Utilities
   */
  public interface BufferUtilities {
    /**
     * TBD.
     */
    @Polyglot public fun byteLength(string: String, encoding: String? = null): Int

    /**
     * TBD.
     */
    @Polyglot public fun compare(buf1: Buffer, buf2: Buffer): Int

    /**
     * TBD.
     */
    @Polyglot public fun concat(list: Array<Buffer>, totalLength: Int? = null): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun copyBytesFrom(view: Buffer, offset: Int = 0, length: Int = view.length): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun isBuffer(obj: Any): Boolean

    /**
     * TBD.
     */
    @Polyglot public fun isEncoding(encoding: String): Boolean

    /**
     * TBD.
     */
    @get:Polyglot public val poolSize: Int
  }

  /**
   * ## Buffer: Factories
   */
  public interface BufferFactories : BufferUtilities {
    /**
     * TBD.
     */
    @Polyglot public fun alloc(size: Int, fill: Value? = null, encoding: String? = null): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun allocUnsafe(size: Int): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun allocUnsafeSlow(size: Int): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun from(array: Array<Byte>): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun from(arrayBuffer: Value, byteOffset: Int = 0, length: Int? = null): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun from(buffer: Buffer): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun from(obj: Value, offsetOrEncoding: Value? = null, length: Value? = null): Buffer

    /**
     * TBD.
     */
    @Polyglot public fun from(string: String, encoding: String? = null): Buffer
  }

  /**
   * ## Buffer: Statics
   */
  public interface BufferStatics : BufferConstructors, BufferFactories

  /**
   * TBD.
   */
  @get:Polyglot public val length: Int

  override fun getMemberKeys(): Array<String> = BUFFER_KEYS
  override fun hasMember(key: String): Boolean = key in BUFFER_KEYS
  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Buffer objects are read-only.")
  }

  /** Host-side factory methods. */
  public companion object {
    /**
     * Create a buffer wrapping the provided [byteArray].
     *
     * @param byteArray The byte array to wrap.
     * @return A new buffer wrapping the provided byte array.
     */
    @JvmStatic public fun of(byteArray: ByteArray): Buffer = NodeBufferFactory.factory().of(byteArray)

    /**
     * Create a buffer wrapping the provided [buf].
     *
     * @param buf The byte buffer to wrap.
     * @return A new buffer wrapping the provided byte buffer.
     */
    @JvmStatic public fun of(buf: ByteBuffer): Buffer = NodeBufferFactory.factory().of(buf)

    /**
     * Create an empty buffer.
     *
     * @return A new empty buffer.
     */
    @JvmStatic public fun empty(): Buffer = NodeBufferFactory.factory().empty()

    /**
     * Create a mutable buffer wrapping the provided [byteArray].
     *
     * @param byteArray The byte array to wrap.
     * @return A new mutable buffer wrapping the provided byte array.
     */
    @JvmStatic public fun mutable(byteArray: ByteArray): Buffer = NodeBufferFactory.factory().ofMutable(byteArray)

    /**
     * Create a mutable buffer wrapping the provided [buf].
     *
     * @param buf The byte buffer to wrap.
     * @return A new mutable buffer wrapping the provided byte buffer.
     */
    @JvmStatic public fun mutable(buf: ByteBuffer): Buffer = NodeBufferFactory.factory().ofMutable(buf)

    /**
     * Allocate a new buffer with the specified [size].
     *
     * @param size The size of the buffer to allocate.
     * @return A new buffer with the specified size.
     */
    @JvmStatic public fun alloc(size: Int = 0, direct: Boolean? = null): Buffer {
      TODO("`Buffer.alloc` is not implemented yet")
    }
  }
}
