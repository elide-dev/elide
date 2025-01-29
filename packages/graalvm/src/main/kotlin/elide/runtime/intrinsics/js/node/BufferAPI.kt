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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.HostAccess.Implementable
import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Buffer
 * 
 * Defines the types and members of the Node.js `buffer` built-in module.
 * 
 * @see BufferAPI.Blob
 * @see BufferAPI.File
 */
@API public interface BufferAPI : NodeAPI {
  /**
   * Implements the `Blob` type from the Node.js `buffer` built-in module. Blobs are read-only chunks of byte data which
   * can be used to derive buffers, strings, and other objects.
   */
  @DelicateElideApi @Implementable public interface Blob {
    /** The content type of this Blob. Note that this value is informative only. */
    @get:Polyglot public val type: String?

    /** Size in bytes of this blob. */
    @get:Polyglot public val size: Int

    /**
     * Returns a promise which resolves with a JavaScript `ArrayBuffer` object containing a copy of this blob's data.
     */
    @Polyglot public fun arrayBuffer(): JsPromise<PolyglotValue>

    /**
     * Returns a copy of this blob between the requested indices, and optionally with a different [type].
     */
    @Polyglot public fun slice(start: Int? = null, end: Int? = null, type: String? = null): Blob

    /**
     * Returns a promise which resolves with the result of decoding this blob's data as a UTF-8 string.
     */
    @Polyglot public fun text(): JsPromise<String>

    /**
     * Returns a stream that can be used to read the contents of this blob.
     */
    @Polyglot public fun stream(): ReadableStream
  }

  /**
   * Implements the `File` class from the Node.js `buffer` built-in module. This type is basically a specialized
   * [Blob] with additional [name] and [lastModified] fields.
   */
  @DelicateElideApi public interface File : Blob {
    /** The name of the file. */
    @get:Polyglot public val name: String

    /** An epoch timestamp indicating the last modification to this file. */
    @get:Polyglot public val lastModified: Long
  }

  /** Decodes a string of Base64-encoded data into bytes, and encodes those bytes into a string using Latin-1. */
  @Polyglot public fun atob(data: Value): String

  /** Decodes a string into bytes using Latin01, end encodes those bytes into a string using Base64. */
  @Polyglot public fun btoa(data: Value): String

  /** Returns whether the provided [input] contains only valid ASCII-encoded data, or is empty. */
  @Polyglot public fun isAscii(input: Value): Boolean

  /** Returns whether the provided [input] contains only valid UTF-8-encoded data, or is empty. */
  @Polyglot public fun isUtf8(input: Value): Boolean

  /**
   * Resolves a 'blob:nodedata:...' an associated Blob object registered using a prior call to URL.createObjectURL().
   */
  @Polyglot public fun resolveObjectUrl(id: String): Value

  /**
   * Re-encodes the given Buffer or Uint8Array instance from one character encoding to another. Returns a new Buffer
   * instance. Supported encodings are 'ascii', 'utf8, 'utf16le', 'ucs2', 'latin1', 'binary'.
   */
  @Polyglot public fun transcode(source: Value, fromEnc: String, toEnc: String): Value
}
