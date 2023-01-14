package elide.runtime.gvm.internals.vfs

import elide.data.CompressionMode
import elide.proto.api.Named
import elide.proto.api.Symbolic

/**
 * # VFS: Compression
 *
 * Wraps a Flatbuffers [CompressionMode] integer with its name, so that it can be debugged and symbolically resolved.
 * See [CompressionMode] for more information about supported algorithms.
 *
 * @see CompressionMode for an enumeration of supported algorithms.
 */
@JvmInline internal value class Compression private constructor (private val mode: Pair<Int, String>) :
  Symbolic<Int>,
  Named {
  /** @return Integer symbol for this compression mode. */
  override val symbol: Int get() = mode.first

  /** @return Name of this compression mode. */
  override val name: String get() = mode.second

  /** @return Name formatted into a debug string. */
  override fun toString(): String = "Compression($name)"

  internal companion object : Symbolic.Resolver<Int, Compression> {
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
    @JvmStatic override fun resoleSymbol(symbol: Int): Compression = when (symbol) {
      CompressionMode.IDENTITY -> IDENTITY
      CompressionMode.GZIP -> GZIP
      CompressionMode.BROTLI -> BROTLI
      CompressionMode.DEFLATE -> DEFLATE
      CompressionMode.SNAPPY -> SNAPPY
      else -> throw unresolved(symbol)
    }
  }
}
