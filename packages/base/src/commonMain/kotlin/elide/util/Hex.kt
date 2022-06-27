package elide.util

import kotlin.math.min

/** Provides cross-platform utilities for encoding values into hex, or decoding values from hex. */
object Hex {
  /** Array of hex-allowable characters.  */
  private val HEX_ARRAY = "0123456789abcdef".toCharArray()

  /**
   * Convert a byte array to hex.
   *
   * @param bytes Raw bytes to encode.
   * @return Resulting hex-encoded string.
   */
  fun bytesToHex(bytes: ByteArray): String? {
    return bytesToHex(bytes, -1)
  }

  /**
   * Convert a byte array to hex, optionally limiting the number of characters returned and cycles performed.
   *
   * @param bytes Raw bytes to encode.
   * @param maxChars Max number of output characters to care about. Pass `-1` to encode the whole thing.
   * @return Resulting hex-encoded string.
   */
  fun bytesToHex(bytes: ByteArray, maxChars: Int): String? {
    val hexChars = CharArray(bytes.size * 2)
    for (j in 0 until if (maxChars == -1) bytes.size else min(bytes.size, maxChars / 2)) {
      val v = bytes[j].toInt() and 0xFF
      hexChars[j * 2] = HEX_ARRAY[v ushr 4]
      hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
    }
    return hexChars.concatToString().trim { it <= ' ' }.replace("\u0000", "")
  }
}
