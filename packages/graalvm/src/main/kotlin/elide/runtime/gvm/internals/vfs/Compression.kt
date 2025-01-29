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
@file:Suppress(
  "MemberVisibilityCanBePrivate",
  "unused",
)

package elide.runtime.gvm.internals.vfs

import tools.elide.data.CompressionMode
import elide.core.api.Symbolic
import elide.core.api.Symbolic.SealedResolver
import elide.proto.api.Named

/**
 * # VFS: Compression
 *
 * Wraps a Flatbuffers [CompressionMode] integer with its name, so that it can be debugged and symbolically resolved.
 * See [CompressionMode] for more information about supported algorithms.
 *
 * @see CompressionMode for an enumeration of supported algorithms.
 */
@JvmInline internal value class Compression private constructor (private val mode: Pair<CompressionMode, String>) :
  Symbolic<Int>,
  Named {
  /** @return Integer symbol for this compression mode. */
  override val symbol: Int get() = mode.first.number

  /** @return Name of this compression mode. */
  override val name: String get() = mode.second

  /** @return Name formatted into a debug string. */
  override fun toString(): String = "Compression($name)"

  internal companion object : SealedResolver<Int, Compression> {
    /** Alias for `IDENTITY` mode (no compression). */
    internal val IDENTITY = Compression(CompressionMode.IDENTITY to "IDENTITY")

    /** Alias for `GZIP` mode (no compression). */
    internal val GZIP = Compression(CompressionMode.GZIP to "GZIP")

    /** Alias for `BROTLI` mode (no compression). */
    internal val BROTLI = Compression(CompressionMode.BROTLI to "BROTLI")

    /** Alias for `DEFLATE` mode (no compression). */
    internal val DEFLATE = Compression(CompressionMode.DEFLATE to "DEFLATE")

    /** Alias for `SNAPPY` mode (no compression). */
    internal val SNAPPY = Compression(CompressionMode.SNAPPY to "SNAPPY")

    /** Sets the default compression mode to apply. */
    internal val DEFAULT = GZIP

    /** Resolve the provided [symbol] to a validated [Compression] instance. */
    @JvmStatic override fun resolve(symbol: Int): Compression = when (symbol) {
      CompressionMode.IDENTITY.number -> IDENTITY
      CompressionMode.GZIP.number -> GZIP
      CompressionMode.BROTLI.number -> BROTLI
      CompressionMode.DEFLATE.number -> DEFLATE
      CompressionMode.SNAPPY.number -> SNAPPY
      else -> throw unresolved(symbol)
    }
  }
}
