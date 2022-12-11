package elide.core.encoding

import kotlin.jvm.JvmInline

/**
 * # Hex
 *
 * Provides cross-platform utilities for encoding values into hex, or decoding values from hex. Available on any target
 * platform supported by Elide/Kotlin, including native platforms.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") public object Hex : Codec<Hex.HexData> {
  /** Carrier value-class for hex-encoded data. */
  @JvmInline public value class HexData constructor (private val encoded: String) : EncodedData {
    /** @inheritDoc */
    override val encoding: Encoding get() = Encoding.HEX

    /** @inheritDoc */
    override val string: String get() = encoded

    /** @inheritDoc */
    override val data: ByteArray get() = encoded.encodeToByteArray()
  }

  /** Array of hex-allowable characters.  */
  public val CHARACTER_SET: CharArray = "0123456789abcdef".toCharArray()

  // Convert a byte array to a hex-encoded string.
  internal fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
      val v = bytes[j].toInt() and 0xFF
      hexChars[j * 2] = CHARACTER_SET[v ushr 4]
      hexChars[j * 2 + 1] = CHARACTER_SET[v and 0x0F]
    }
    return hexChars.concatToString().trim { it <= ' ' }.replace("\u0000", "")
  }

  // Internal implementation of hex decoding from strings to strings.
  internal fun hexToString(hex: String): String {
    val result = ByteArray(hex.length / 2)
    for (i in hex.indices step 2) {
      val firstIndex = CHARACTER_SET.indexOf(hex[i])
      val secondIndex = CHARACTER_SET.indexOf(hex[i + 1])
      val octet = firstIndex.shl(4).or(secondIndex)
      result[i.shr(1)] = octet.toByte()
    }
    return result.decodeToString()
  }

  /** @inheritDoc */
  override fun encoding(): Encoding = Encoding.HEX

  /** @inheritDoc */
  override fun encode(data: ByteArray): HexData {
    return HexData(bytesToHex(data))
  }

  /** @inheritDoc */
  override fun decodeBytes(data: ByteArray): ByteArray = hexToString(data.decodeToString()).encodeToByteArray()
}
