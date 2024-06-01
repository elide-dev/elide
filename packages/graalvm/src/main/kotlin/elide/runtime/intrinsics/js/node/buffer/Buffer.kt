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
}
