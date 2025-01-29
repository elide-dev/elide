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
package elide.runtime.intrinsics.js.node.buffer

import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * Provides the prototype for the Node.js `Buffer` type, and acts as a meta-object for it, allowing the creation of new
 * instances via "static" methods. A single instance of this class should be injected into a guest context to act as
 * the `Buffer` symbol itself.
 */
@DelicateElideApi public interface BufferClass : ProxyInstantiable, ProxyObject {
  /**
   * The size of the internal pool used for unsafe buffers. This value has no effect in the host implementation as NIO
   * buffers with a managed pool are used instead.
   */
  public var poolSize: Int

  /**
   * Allocate a new buffer of the given [size], optionally using the data from [fill] and, if the value is a string,
   * converting it to bytes using a specific [encoding]. The buffer's bytes are initialized to zero.
   */
  public fun alloc(size: Int, fill: PolyglotValue? = null, encoding: String? = null): BufferInstance

  /**
   * Allocate a new buffer of the given [size] without initializing it. The returned buffer contains arbitrary values
   * and must be manually zeroed out.
   *
   * Whether this method actually uses a pre-allocated pool is not guaranteed, the current implementation uses the
   * same mechanism as [alloc].
   */
  public fun allocUnsafe(size: Int): BufferInstance

  /**
   * Allocate a new buffer of the given [size] without initializing it. The returned buffer contains arbitrary values
   * and must be manually zeroed out.
   *
   * Whether this method actually uses a pre-allocated pool is not guaranteed, the current implementation uses the
   * same mechanism as [alloc].
   */
  public fun allocUnsafeSlow(size: Int): BufferInstance

  /**
   * Returns the length of a [string] when converted to bytes using the given [encoding] (defaults to UTF-8). Note that
   * per the Node.js API documentation, [string] may also be a `Buffer`, `ArrayBuffer`, `DataView`, or `TypedArray`, in
   * which case the value of the `byteLength` property is returned instead.
   */
  public fun byteLength(string: PolyglotValue, encoding: String?): Int

  /**
   * Compare two instances of Buffer or UInt8Array.
   */
  public fun compare(buf1: PolyglotValue, buf2: PolyglotValue): Int

  /**
   * Allocate a new buffer and fill it by concatenating the data from a [list] of values (either Buffer or Uint8Array
   * instances), up to [totalLength] if specified.
   */
  public fun concat(list: PolyglotValue, totalLength: Int? = null): BufferInstance

  /**
   * Allocates a new Buffer instance using the data from a [view] (a `TypedArray` object). Optional [offset] and
   * [length] parameters can be used to restrict the window of data copied from the view.
   */
  public fun copyBytesFrom(view: PolyglotValue, offset: Int? = null, length: Int? = null): BufferInstance

  /**
   * Allocate a new buffer and fill it using the data from the given [source]. The source may be a simple `integer[]`
   * array, Buffer or UInt8Array instance, in which case other parameters are ignored; an `ArrayBuffer` or
   * `SharedArrayBuffer`, which allow setting an [offset] and [length] for the data to copy; or a string, which is
   * converted to bytes using the [encoding].
   *
   * Using objects as a source is not yet supported.
   */
  public fun from(
    source: PolyglotValue,
    offset: Int? = null,
    length: Int? = null,
    encoding: String? = null
  ): BufferInstance

  /**
   * Returns whether a given object is a Buffer instance.
   */
  public fun isBuffer(obj: PolyglotValue): Boolean

  /**
   * Returns whether a given string represents a valid encoding supported by the runtime.
   */
  public fun isEncoding(encoding: String): Boolean
}
