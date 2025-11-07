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
package elide.runtime.http.server.common

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.graalvm.polyglot.Value
import kotlinx.serialization.json.Json
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.gvm.internals.serialization.GuestValueSerializer
import elide.runtime.http.server.common.WorkerResponseContent.mapElement

/**
 * Utilities used to map the content of a response returned by a Workers-style guest server application.
 *
 * Use [mapElement] to return a [ByteBuf] with the default mapping for a single content chunk.
 */
internal object WorkerResponseContent {
  /** Maps the given [value] to a string and returns its UTF-8 encoded bytes. */
  @JvmStatic internal fun mapString(value: Value): ByteBuf {
    return Unpooled.copiedBuffer(value.asString(), Charsets.UTF_8)
  }

  /** Encodes a [value] as JSON, then encode the result as UTF-8 and return the bytes. */
  @JvmStatic internal fun mapJSON(value: Value): ByteBuf {
    return Unpooled.copiedBuffer(Json.encodeToString(GuestValueSerializer, value), Charsets.UTF_8)
  }

  /** Map a buffer-like [value], copying its bytes into a new buffer and returning it. */
  @JvmStatic internal fun mapBuffer(value: Value): ByteBuf {
    val buffer = ByteArray(value.bufferSize.toInt())
    value.readBuffer(0, buffer, 0, buffer.size)
    return Unpooled.wrappedBuffer(buffer)
  }

  /**
   * Select the preferred mapping for a guest [value] and return the resulting bytes:
   *
   * - Strings are encoded using the UTF-8 charset.
   * - Buffer-like values have their data copied and wrapped as a buffer.
   * - JavaScript ArrayBufferView values are copied into a new buffer.
   * - Arrays, map-like values, and objects (values with members) are JSON-encoded.
   * - If none of the above rules match, [toString] is called and the result is encoded.
   */
  @JvmStatic internal fun mapElement(value: Value): ByteBuf = when {
    value.isString -> mapString(value)
    value.hasBufferElements() -> mapBuffer(value)
    ArrayBufferViews.getViewTypeOrNull(value) != null -> {
      Unpooled.wrappedBuffer(ArrayBufferViews.readViewedBytes(value))
    }

    value.hasArrayElements() || value.hasHashEntries() || value.hasMembers() -> mapJSON(value)
    else -> Unpooled.copiedBuffer(value.toString(), Charsets.UTF_8)
  }
}
