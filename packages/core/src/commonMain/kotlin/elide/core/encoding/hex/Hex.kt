package elide.core.encoding.hex

import elide.core.encoding.Codec
import elide.core.encoding.Encoding

/**
 * # Hex
 *
 * Provides cross-platform utilities for encoding values into hex, or decoding values from hex. Available on any target
 * platform supported by Elide/Kotlin, including native platforms.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") public expect object Hex : Codec<HexData> {
  /** @inheritDoc */
  override fun encoding(): Encoding

  /** @inheritDoc */
  override fun encode(data: ByteArray): HexData

  /** @inheritDoc */
  override fun decodeBytes(data: ByteArray): ByteArray
}
