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
 */
@API public interface BufferAPI : NodeAPI {
  @DelicateElideApi @Implementable public interface Blob {
    @get:Polyglot public val type: String?

    @get:Polyglot public val size: Int

    @Polyglot public fun arrayBuffer(): JsPromise<PolyglotValue>

    @Polyglot public fun slice(start: Int? = null, end: Int? = null, type: String? = null): Blob

    @Polyglot public fun text(): JsPromise<String>

    @Polyglot public fun stream(): ReadableStream
  }

  @DelicateElideApi public interface File : Blob {
    @get:Polyglot public val file: String

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
