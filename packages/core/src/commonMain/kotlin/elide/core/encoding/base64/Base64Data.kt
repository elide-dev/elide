package elide.core.encoding.base64

import elide.core.encoding.EncodedData
import elide.core.encoding.Encoding
import kotlin.jvm.JvmInline


/** Carrier value-class for base64-encoded data. */
@JvmInline public value class Base64Data constructor (override val data: ByteArray): EncodedData {
  /** @inheritDoc */
  override val encoding: Encoding get() = Encoding.HEX

  /** @inheritDoc */
  override val string: String get() = data.decodeToString()
}
