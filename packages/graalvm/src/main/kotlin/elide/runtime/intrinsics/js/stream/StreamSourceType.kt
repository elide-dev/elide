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
package elide.runtime.intrinsics.js.stream

import elide.core.api.Symbolic

/** String expected for byte stream sources. */
public const val STREAM_SOURCE_TYPE_BYTES: String = "bytes"

/** String expected for default stream sources. */
public const val STREAM_SOURCE_TYPE_DEFAULT: String = "default"

/**
 * ## Stream Source Type
 *
 * An enumeration of possible states for the [ReadableStreamSource.type] property; these values surface to guest code as
 * strings.
 */
public enum class StreamSourceType (override val symbol: String) : Symbolic<String> {
  /**
   * ## `bytes`
   *
   * This value is passed as part of a [ReadableStreamSource] when the controller is of type
   * [ReadableByteStreamController].
   */
  BYTES(STREAM_SOURCE_TYPE_BYTES),

  /**
   * ## `default`
   *
   * This value is passed as part of a [ReadableStreamSource] when the controller is of type
   * [ReadableStreamDefaultController].
   */
  DEFAULT(STREAM_SOURCE_TYPE_DEFAULT);

  /** Resolves a [StreamSourceType] from a raw string. */
  public companion object : Symbolic.SealedResolver<String, StreamSourceType> {
    override fun resolve(symbol: String): StreamSourceType = when (symbol) {
      STREAM_SOURCE_TYPE_BYTES -> BYTES
      STREAM_SOURCE_TYPE_DEFAULT -> DEFAULT
      else -> throw unresolved(symbol)
    }
  }
}
