package elide.core.encoding.hex

import elide.core.encoding.EncodedData
import elide.core.encoding.Encoding
import kotlin.jvm.JvmInline

/** Carrier value-class for hex-encoded data. */
@JvmInline public value class HexData constructor (private val encoded: String) : EncodedData {
  /** @inheritDoc */
  override val encoding: Encoding get() = Encoding.HEX

  /** @inheritDoc */
  override val string: String get() = encoded

  /** @inheritDoc */
  override val data: ByteArray get() = encoded.encodeToByteArray()
}
